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
case class ItemResponse(id: String, value: String, outcome: String)

object ItemResponse {


  implicit object ItemResponseWrites extends Writes[ItemResponse] {
    def writes(response: ItemResponse) = {
      JsObject(
        List(
          "id" -> JsString(response.id.toString),
          "value" -> JsString(response.value.toString),
          "outcome" -> JsString(response.outcome.toString)
        )
      )
    }
  }


  implicit object ItemResponseReads extends Reads[ItemResponse] {

    def reads(json: JsValue):ItemResponse = {

      new ItemResponse(
        (json \ "id").asOpt[String].getOrElse(""),
        (json \ "value").asOpt[String].getOrElse(""),
        (json \ "outcome").asOpt[String].getOrElse("")
      )

    }
  }

}
