package org.corespring.poc.integration.impl.transformers

import org.bson.types.ObjectId
import org.corespring.platform.core.models.itemSession.ItemSession
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qti.models.responses.{StringResponse, ArrayResponse, Response}
import play.api.Logger
import play.api.libs.json._

trait SessionDocumentTransformer{
  /**
   * Convert a session document to v2 json. This involves
   * converting the legacy <ItemSession>#responses property to
   * 'components'.
   * @param sessionDoc - the complete session document.
   * @return
   */
  def toV2(sessionDoc:JsValue) : JsValue

  /**
   * Convert v2 json to a session document.
   * v2 Json is a subset of the full document so it'll need
   * to be merged with the existing document. Note that
   * @param v2Json - the update
   * @param sessionDoc - the document to merge with
   * @return
   */
  def toSessionDoc(v2Json:JsValue,sessionDoc: JsValue) : JsValue
}

trait ItemSessionTransformer {
  def toItemSession(v2Json: JsValue): ItemSession
  def toV2Session(itemSession:ItemSession): JsValue
}

object ItemSessionTransformer extends ItemSessionTransformer {

  private val logger = Logger("poc.integration")

  def toV2Session(session: ItemSession): JsValue = {

    def components: JsObject = {

      def response(r: Response): JsValue = {
        r match {
          case StringResponse(id, value, _) => JsString(value)
          case ArrayResponse(id, value, _) => JsArray(value.map(JsString(_)))
        }
      }

      val out: Seq[(String, JsValue)] = session.responses.map {
        r => (r.id, Json.obj( "answers" -> response(r)))
      }

      JsObject(out)
    }

    Json.obj(
      "id" -> JsString(session.id.toString),
      "itemId" -> JsString(session.itemId.toString),
      "components" -> components
    )
  }

  /**
   */
  def toItemSession(v2Session: JsValue): ItemSession = {

    logger.debug(Json.stringify(v2Session))

    val itemId = VersionedId((v2Session \ "itemId").as[String]).getOrElse(throw new RuntimeException("Invalid versioned id"))

    def makeResponses(answers: JsObject): Seq[Response] = answers.fields.map {
      (f: (String, JsValue)) =>
        val (key, json) = f
        (json \ "answers") match {
          case jsonArray: JsArray => ArrayResponse(id = key, responseValue = jsonArray.as[Seq[String]])
          case jsonString: JsString => StringResponse(id = key, responseValue = jsonString.as[String])
          case _ => {
            throw new RuntimeException(s"Unknown response format id: $itemId: ${Json.stringify(json)}")
          }
        }
    }

    val responses = makeResponses((v2Session \ "components").as[JsObject])

    val out = ItemSession(
      id = new ObjectId((v2Session \ "id").as[String]),
      itemId = itemId,
      responses = responses
    )
    out
  }
}
