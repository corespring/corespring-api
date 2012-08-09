package models

import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import org.bson.types.ObjectId
import se.radley.plugin.salat._
import mongoContext._
import org.joda.time.DateTime
import play.api.libs.json.{JsArray, JsString, JsObject, Writes}
import play.api.Play.current



/**
 * Case class representing an individual item session
 */
case class ItemSession (
                         id: ObjectId = new ObjectId(),
                         var start: DateTime = new DateTime(),
                         var finish: DateTime = new DateTime(),
                         var responses: List[ItemResponse] = List[ItemResponse]())


/**
 * Companion object for ItemSession.
 * All operations specific to ItemSession are handled here
 *
 */
object ItemSession extends ModelCompanion[ItemSession,ObjectId] {
  val collection = mongoCollection("itemsession")
  val dao = new SalatDAO[ItemSession, ObjectId](collection = collection) {}


  /**
   * Json Serializer
   */
  implicit object ItemSessionWrites extends Writes[ItemSession] {
    def writes(session: ItemSession) = {
      JsObject(
        List(
          "id" -> JsString(session.id.toString),
          "start" -> JsString(session.start.getMillis.toString),
          "finish" -> JsString(session.finish.getMillis.toString)
        )
      )
    }
  }

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

}


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



