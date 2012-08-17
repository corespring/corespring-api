package models

import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import org.bson.types.ObjectId
import se.radley.plugin.salat._
import mongoContext._
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.Play.current
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import org.codehaus.jackson.annotate.JsonValue


/**
 * Case class representing an individual item session
 */
case class ItemSession (
                         var id: Option[ObjectId],
                         var itemId: Option[ObjectId],
                         var start: DateTime = new DateTime(),
                         var finish: DateTime = new DateTime(),
                         var responses: List[ItemResponse] = List[ItemResponse]()
                        )


/**
 * Companion object for ItemSession.
 * All operations specific to ItemSession are handled here
 *
 */
object ItemSession extends ModelCompanion[ItemSession,ObjectId] {
  val collection = mongoCollection("itemsessions")
  val dao = new SalatDAO[ItemSession, ObjectId](collection = collection) {}


  /**
   * Json Serializer
   */
  implicit object ItemSessionWrites extends Writes[ItemSession] {
    def writes(session: ItemSession) = {

      // is the id set?
      session.id match {
        case Some(id) => {
          JsObject(
            List(
              "id" -> JsString(session.id.get.toString),
              "start" -> JsString(session.start.getMillis.toString),
              "finish" -> JsString(session.finish.getMillis.toString),
              "responses" -> Json.toJson(session.responses)
            )
          )
        }
        case _ => {
          JsObject(
            List(
              "start" -> JsString(session.start.getMillis.toString),
              "finish" -> JsString(session.finish.getMillis.toString),
              "responses" -> Json.toJson(session.responses)
            )
          )
        }
      }


    }
  }

  implicit object ItemSessionReads extends Reads[ItemSession] {
    // TODO  implement me
    def reads(json: JsValue) = null
  }



}






