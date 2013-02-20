package models

import play.api.libs.json._
import play.api.libs.json.JsString
import play.api.libs.json.JsObject
import play.api.libs.json.Json._
import com.novus.salat.annotations.raw.Salat
import qti.models.QtiItem

/**
 * Case class representing a user's response to an indvidual qusetion in an item
 * {{{
 * {
 * id: "question1",
 * value: "a",
 * outcome: {
 * score: 1,
 * report: {
 * a: true
 * }
 * }
 * }
 * }}}
 *
 * @param id  the id defined in the QTI markup that identifies the question within the item
 * @param outcome  this is the outcome of the user interaction as calculated by the server. usually 'SCORE' property
 */

@Salat
abstract class ItemResponse(val id: String, val outcome: Option[ItemResponseOutcome] = None) {
  def value: String

  def getIdValueIndex: Seq[(String, String, Int)]
}

case class StringItemResponse(override val id: String, responseValue: String, override val outcome: Option[ItemResponseOutcome] = None) extends ItemResponse(id, outcome) {
  override def value = responseValue

  /**
   * Return the response as a sequence of id, value, index
   * @return
   */
  def getIdValueIndex = Seq((id, responseValue, 0))

}

case class ArrayItemResponse(override val id: String, responseValue: Seq[String], override val outcome: Option[ItemResponseOutcome] = None) extends ItemResponse(id, outcome) {
  override def value = responseValue.mkString(",")

  def getIdValueIndex = responseValue.view.zipWithIndex.map((f: (String, Int)) => (id, f._1, f._2))
}

case class ItemResponseOutcome(score: Float = 0, comment: Option[String] = None, outcomeProperties: Map[String, Boolean] = Map()) {
  def isCorrect = score == 1

  def getOutcomeBasedFeedbackContents(qti: QtiItem, responseIdentifier: String): Map[String, String] = {
    val modalFeedbacks = qti.modalFeedbacks;
    val feedbackBlocks = qti.itemBody.feedbackBlocks
    val feedbacks = (modalFeedbacks ++ feedbackBlocks).filter(_.outcomeIdentifier == responseIdentifier)
    outcomeProperties.map(prop => {
      feedbacks.find(_.outcomeAttrs.contains(prop._1) && prop._2) match {
        case Some(fi) => (fi.csFeedbackId -> fi.content)
        case None => ("" -> "")
      }
    }).filter(_._1.nonEmpty).toMap[String, String]
  }
}

object ItemResponseOutcome {

  implicit object Writes extends Writes[ItemResponseOutcome] {
    def writes(iro: ItemResponseOutcome): JsValue = {
      var jsseq: Seq[(String, JsValue)] = Seq("score" -> JsNumber(iro.score))
      if (iro.comment.isDefined) jsseq = jsseq :+ ("comment" -> JsString(iro.comment.get))
      jsseq = jsseq ++ iro.outcomeProperties.toSeq.map(prop => (prop._1 -> JsBoolean(prop._2)))
      JsObject(jsseq)
    }
  }

}

object ItemResponse {
  val value = "value"
  val id = "id"
  val outcome = "outcome"
  val report = "report"

  def apply(r: ItemResponse, outcome: Option[ItemResponseOutcome]): ItemResponse =
    r match {
      case StringItemResponse(i, v, out) => StringItemResponse(i, v, outcome)
      case ArrayItemResponse(i, v, out) => ArrayItemResponse(i, v, outcome)
    }

  def containsValue(r: ItemResponse, s: String): Boolean = r match {
    case StringItemResponse(_, v, _) => s == v
    case ArrayItemResponse(_, v, _) => v.contains(s)
  }

  implicit object ItemResponseWrites extends Writes[ItemResponse] {
    def writes(response: ItemResponse) = {

      val seq: Seq[Option[(String, JsValue)]] = response match {
        case StringItemResponse(id, v, outcome) => {
          Seq(Some("id" -> JsString(id)),
            Some("value" -> JsString(v)),
            outcome.map(("outcome" -> toJson(_)))
          )
        }
        case ArrayItemResponse(id, v, outcome) => {
          Seq(
            Some("id" -> JsString(id)),
            Some("value" -> JsArray(v.map(JsString(_)))),
            outcome.map(("outcome" -> toJson(_)))
          )
        }
      }
      JsObject(seq.flatten)
    }
  }


  implicit object ItemResponseReads extends Reads[ItemResponse] {

    /**
     * We don't read the outcome from json - its generated from the qti
     * @param json
     * @return
     */
    def reads(json: JsValue): ItemResponse = {

      val id = (json \ "id").as[String]

      (json \ "value") match {
        case JsArray(seq) => ArrayItemResponse(id, seq.map(_.as[String]))
        case JsString(s) => StringItemResponse(id, s)
        case _ => StringItemResponse(id, (json \ "value").as[String])
      }
    }
  }

}


case class ItemResponseAggregate(val id: String, correctAnswers: Seq[String], numCorrect: Int = 0, numResponses: Int = 0, choices: Map[String, Int] = Map()) {
  def aggregate(response: ItemResponse): ItemResponseAggregate = {
    val isCorrect = response.outcome.get.isCorrect
    def numFor(s: String): Int = if (choices.contains(s)) choices(s) + 1 else 1
    response match {
      case sr: StringItemResponse =>
        ItemResponseAggregate(id, correctAnswers, if (isCorrect) numCorrect + 1 else numCorrect, numResponses + 1, choices + (sr.value -> numFor(sr.value)))

      case ar: ArrayItemResponse =>
        ItemResponseAggregate(id, correctAnswers, if (isCorrect) numCorrect + 1 else numCorrect, numResponses + ar.responseValue.length, choices ++ ar.responseValue.map(p => (p -> numFor(p))))
    }
  }
}

object ItemResponseAggregate {

  implicit object ItemResponseWrites extends Writes[ItemResponseAggregate] {
    def writes(agg: ItemResponseAggregate) = {
      var list = List[(String, JsValue)]()
      list = ("id" -> JsString(agg.id)) :: list
      list = ("numCorrectResponses" -> JsNumber(agg.numCorrect)) :: list
      list = ("totalResponses" -> JsNumber(agg.numResponses)) :: list
      list = ("choices" -> toJson(agg.choices)) :: list
      list = ("correctAnswers" -> toJson(agg.correctAnswers)) :: list
      JsObject(list)
    }
  }

}
