package models

import play.api.libs.json._
import play.api.libs.json.JsString
import play.api.libs.json.JsObject

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
case class ItemResponse(id: String, value: String, outcome: String = "")

object ItemResponse {
   val value = "value"
   val id = "id"
   val outcome = "outcome"

   private val Delimiter = " _item_response_delimiter_ "

  implicit object ItemResponseWrites extends Writes[ItemResponse] {
    def writes(response: ItemResponse) = {

      def createValue( storedValue : String ) : JsValue = {
        storedValue.split(Delimiter).toSeq match {
          case Seq(s) => JsString(s)
          case l : Seq[_]  => JsArray(l.map( JsString(_)))
        }
      }

      JsObject(
        List(
          "id" -> JsString(response.id.toString),
          "value" -> createValue(response.value.toString) ,
          "outcome" -> JsString(response.outcome.toString)
        )
      )
    }
  }


  implicit object ItemResponseReads extends Reads[ItemResponse] {

    def reads(json: JsValue):ItemResponse = {

      //The value can either be a single string or an array of strings
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
        value = getValue( (json\"value")),
        outcome = (json \ "outcome").asOpt[String].getOrElse("")
      )

    }
  }

}
