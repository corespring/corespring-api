package models

import play.api.libs.json._
import play.api.libs.json.JsString
import play.api.libs.json.JsObject
import play.api.libs.json.Json._
import com.novus.salat.annotations.raw.Salat

/**
 * Case class representing a user's response to an indvidual qusetion in an item
 *
 * e.g.
 * {
 * id: "question1",
 * value: "choice1",
 * outcome: {
 * "$score": 1
 * }
 * }
 *
 * @param id  the id defined in the QTI markup that identifies the question within the item
 * @param outcome  this is the outcome of the user interaction as calculated by the server. usually 'SCORE' property
 */

@Salat
abstract class ItemResponse(val id: String, val outcome: Option[ItemResponseOutcome] = None)

case class StringItemResponse(override val id: String, responseValue: String, override val outcome: Option[ItemResponseOutcome] = None) extends ItemResponse(id, outcome)

case class ArrayItemResponse(override val id: String, responseValue: Seq[String], override val outcome: Option[ItemResponseOutcome] = None) extends ItemResponse(id, outcome)

case class ItemResponseOutcome(score: Float = 0, comment: Option[String] = None) {
  def isCorrect = score == 1
}

object ItemResponseOutcome {



  implicit object Writes extends Writes[ItemResponseOutcome] {
    def writes(iro: ItemResponseOutcome): JsValue = {
      val json = com.codahale.jerkson.Json.generate(iro)
      Json.parse(json)
    }
  }

}

object ItemResponse {
  val value = "value"
  val id = "id"
  val outcome = "outcome"

  def apply(r: ItemResponse, outcome: ItemResponseOutcome): ItemResponse =
    r match {
      case StringItemResponse(i, v, out) => StringItemResponse(i, v, Some(outcome))
      case ArrayItemResponse(i, v, out) => ArrayItemResponse(i, v, Some(outcome))
    }

  def containsValue(r:ItemResponse, s : String) : Boolean = r match {
    case StringItemResponse(_, v, _) => s == v
    case ArrayItemResponse(_, v, _) => v.contains(s)
  }

  /**
   * Saving the array with a simple delimiter like ',' could cause problems
   * As there should be used in the answer if its a single value.
   * Instead use this delimiter to guarantee that the items are read/written correctly as arrays if needed.
   */

  implicit object ItemResponseWrites extends Writes[ItemResponse] {
    def writes(response: ItemResponse) = {

      val seq : Seq[Option[(String,JsValue)]] = response match {
        case StringItemResponse(id,v,outcome) => {
          Seq(Some("id" -> JsString(id)),
            Some("value" -> JsString(v)),
            outcome.map(("outcome" -> toJson(_)))
          )
        }
        case ArrayItemResponse(id,v,outcome) => {
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

      val id = (json\"id").as[String]

      (json\"value") match {
        case JsArray(seq) => ArrayItemResponse(id,seq.map(_.as[String]))
        case JsString(s) => StringItemResponse(id, s)
        case _ => StringItemResponse(id, (json\"value").as[String])
      }
    }
  }
}
