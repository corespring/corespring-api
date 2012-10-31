package models

import play.api.libs.json._
import play.api.libs.json.JsString
import play.api.libs.json.JsObject
import play.api.libs.json.Json._

/**
 * Case class representing a user's response to an indvidual qusetion in an item
 *
 * e.g.
 * {
 *      id: "question1",
 *      value: "choice1",
 *      outcome: {
 *          "$score": 1
 *      }
 *  }
 *
 * @param id  the id defined in the QTI markup that identifies the question within the item
 * @param value string response, may be json-formatted. Format and type depends on the question type
 * @param outcome  this is the outcome of the user interaction as calculated by the server. usually 'SCORE' property
 */
case class ItemResponse(id: String, value: String, outcome: Option[ItemResponseOutcome] = None )

case class ItemResponseOutcome( score: Float = 0, maxScore: Float = 0, comment : String = "" ) {
  def isCorrect = score == maxScore
}

object ItemResponseOutcome {
  implicit object Writes extends Writes[ItemResponseOutcome] {
    def writes(iro:ItemResponseOutcome) : JsValue = {
     val json = com.codahale.jerkson.Json.generate(iro)
     Json.parse(json)
    }
  }
}

object ItemResponse {
   val value = "value"
   val id = "id"
   val outcome = "outcome"

  /**
   * Saving the array with a simple delimiter like ',' could cause problems
   * As there should be used in the answer if its a single value.
   * Instead use this delimiter to guarantee that the items are read/written correctly as arrays if needed.
   */
   val Delimiter = " _item_response_delimiter_ "

  implicit object ItemResponseWrites extends Writes[ItemResponse] {
    def writes(response: ItemResponse) = {

      def createValue( storedValue : String ) : JsValue = {
        storedValue.split(Delimiter).toSeq match {
          case Seq(s) => JsString(s)
          case l : Seq[_]  => JsArray(l.map( JsString(_)))
        }
      }

      val seq : Seq[Option[(String,JsValue)]] = List(
        Some("id" -> JsString(response.id.toString)),
        Some("value" -> createValue(response.value.toString)),
        response.outcome.map(("outcome" -> toJson(_)))
      )

      JsObject(seq.flatten)
    }
  }


  implicit object ItemResponseReads extends Reads[ItemResponse] {

    /**
     * We don't read the outcome from json - its generated from the qti
     * @param json
     * @return
     */
    def reads(json: JsValue):ItemResponse = {

      def getValue(js : JsValue) : String = {
        if ( js.asOpt[String].isDefined ){
          js.as[String]
        } else {
          if( js.asOpt[Seq[String]].isDefined){
           js.as[Seq[String]].mkString(Delimiter)
          } else {
            ""
          }
        }
      }

      new ItemResponse(
        id = (json \ "id").asOpt[String].getOrElse(""),
        value = getValue( (json\"value"))
      )

    }
  }

}
