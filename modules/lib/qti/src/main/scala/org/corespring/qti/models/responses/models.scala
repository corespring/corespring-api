package org.corespring.qti.models.responses

import com.novus.salat.annotations.raw.Salat
import org.corespring.qti.models.QtiItem
import play.api.libs.json.JsArray
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json._
import play.api.libs.json._
import scala.Some

@Salat
abstract class Response(val id: String, val outcome: Option[ResponseOutcome] = None) {
  def value: String

  def getIdValueIndex: Seq[(String, String, Int)]
}


case class ResponseOutcome(score: Float = 0, isCorrect: Boolean = false, comment: Option[String] = None, outcomeProperties: Map[String, Boolean] = Map()){

  def getOutcomeBasedFeedbackContents(qti:QtiItem, responseIdentifier:String):Map[String,String] = {
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

object ResponseOutcome{
  implicit object ResponseOutcomeWrites extends Writes[ResponseOutcome] {
    def writes(iro: ResponseOutcome): JsValue = {
      var jsseq:Seq[(String,JsValue)] = Seq("score" -> JsNumber(iro.score), "isCorrect" -> JsBoolean(iro.isCorrect))
      if (iro.comment.isDefined) jsseq = jsseq :+ ("comment" -> JsString(iro.comment.get))
      jsseq = jsseq ++ iro.outcomeProperties.toSeq.map(prop => (prop._1 -> JsBoolean(prop._2)))
      JsObject(jsseq)
    }
  }
}

object Response {
  val value = "value"
  val id = "id"
  val outcome = "outcome"
  val report = "report"

  def combine(r: Response, outcome: Option[ResponseOutcome]): Response =
    r match {
      case StringResponse(i, v, out) => StringResponse(i, v, outcome)
      case ArrayResponse(i, v, out) => ArrayResponse(i, v, outcome)
    }

  def containsValue(r: Response, s: String): Boolean = r match {
    case StringResponse(_, v, _) => s == v
    case ArrayResponse(_, v, _) => v.contains(s)
  }

  implicit object ResponseWrites extends Writes[Response] {
    def writes(response: Response) = {

      val seq: Seq[Option[(String, JsValue)]] = response match {
        case StringResponse(id, v, outcome) => {
          Seq(Some("id" -> JsString(id)),
            Some("value" -> JsString(v)),
            outcome.map(("outcome" -> toJson(_)))
          )
        }
        case ArrayResponse(id, v, outcome) => {
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


  implicit object ResponseReads extends Reads[Response] {

    /**
     * We don't read the outcome from json - its generated from the qti
     * @param json
     * @return
     */
    def reads(json: JsValue): JsResult[Response] = {

      val id = (json \ "id").as[String]

      JsSuccess{(json \ "value") match {
        case JsArray(seq) => ArrayResponse(id, seq.map(_.as[String]))
        case JsString(s) => StringResponse(id, s)
        case _ => StringResponse(id, (json \ "value").as[String])
      }}
    }
  }

}

case class StringResponse(override val id: String, responseValue: String, override val outcome: Option[ResponseOutcome] = None) extends Response(id, outcome) {
  override def value = responseValue

  /**
   * Return the response as a sequence of id, value, index
   * @return
   */
  def getIdValueIndex = Seq((id, responseValue, 0))

}

case class ArrayResponse(override val id: String, responseValue: Seq[String], override val outcome: Option[ResponseOutcome] = None) extends Response(id, outcome) {
  override def value = responseValue.mkString(",")

  def getIdValueIndex = responseValue.view.zipWithIndex.map((f: (String, Int)) => (id, f._1, f._2))
}


case class ResponseAggregate(val id: String, correctAnswers: Seq[String], numCorrect: Int = 0, numResponses: Int = 0, totalDistribution: Int = 0, choices: Map[String, Int] = Map()) {
  def aggregate(response: Response): ResponseAggregate = {
    val isCorrect = response.outcome match {
      case Some(r) => r.isCorrect
      case _ => false
    }
    def numFor(s: String): Int = if (choices.contains(s)) choices(s) + 1 else 1
    response match {
      case sr: StringResponse =>
        ResponseAggregate(id, correctAnswers, if (isCorrect) numCorrect + 1 else numCorrect, numResponses + 1, totalDistribution + 1, choices + (sr.value -> numFor(sr.value)))

      case ar: ArrayResponse =>
        ResponseAggregate(id, correctAnswers, if (isCorrect) numCorrect + 1 else numCorrect, numResponses + 1, totalDistribution + ar.responseValue.length, choices ++ ar.responseValue.map(p => (p -> numFor(p))))
    }
  }
}

object ResponseAggregate {
  def build(id: String, correctResponses: Seq[String], response: Response): ResponseAggregate = {
    ResponseAggregate(id, correctResponses).aggregate(response)
  }


  implicit object AggregrateFormat extends Writes[ResponseAggregate] {
    def writes(agg: ResponseAggregate) = {
      var list = List[(String, JsValue)]()
      list = ("id" -> JsString(agg.id)) :: list
      list = ("numCorrectResponses" -> JsNumber(agg.numCorrect)) :: list
      list = ("totalResponses" -> JsNumber(agg.numResponses)) :: list
      list = ("totalDistribution" -> JsNumber(agg.totalDistribution)) :: list
      list = ("choices" -> toJson(agg.choices)) :: list
      list = ("correctAnswers" -> toJson(agg.correctAnswers)) :: list
      JsObject(list)
    }
  }

}



