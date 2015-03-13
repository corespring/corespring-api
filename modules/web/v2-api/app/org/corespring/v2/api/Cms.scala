package org.corespring.v2.api

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.drafts.item.services.ItemDraftService
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import play.api.Logger
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ Await, ExecutionContext, Future }

/**
 * Some request handlers specific to the CMS.
 */
trait Cms extends Controller {

  private lazy val logger = Logger("org.corespring.v2.api.Cms")

  import scala.concurrent.ExecutionContext.Implicits.global

  def itemTransformer: ItemTransformer

  def itemService: ItemService
  def draftService: ItemDraftService

  def v1ApiCreate: (Request[AnyContent] => Future[SimpleResult])

  /**
   * AC-65.
   * This is a temporary way of allowing the CMS, which is v1 based
   * to create v2 items using it's v1 templates.
   * We create the v1 item using the v1.ItemApi then we remove the 'data' property from the db
   * ensuring that the new item has no v1 data model.
   * @return
   */

  private def addV2ModelAndTrashV1(r: SimpleResult, keepV1Data: Boolean) = {
    import scala.concurrent.duration._
    val bytes: Array[Byte] = Await.result(r.body |>>> Iteratee.consume[Array[Byte]](), 1.second)
    val s = new String(bytes, "utf-8")
    val json = Json.parse(s)
    val id = (json \ "id").as[String]
    itemTransformer.updateV2Json(VersionedId(new ObjectId(id)))

    if (!keepV1Data) {
      def withVersionedId(id: String) = MongoDBObject("_id._id" -> new ObjectId(id))
      val removeData = MongoDBObject("$unset" -> MongoDBObject("data" -> ""))
      itemService.collection.update(withVersionedId(id), removeData, false, false)
    }
  }

  def createItemFromV1Data = Action.async { implicit request =>

    val result: Future[SimpleResult] = v1ApiCreate(request)

    result.map { r =>
      if (r.header.status == OK) {

        val keepV1Data = request.getQueryString("keep-v1-data").exists(_ == "true")
        addV2ModelAndTrashV1(r, keepV1Data)
      }
      r
    }
  }

  def contentFormat(i: Item) = {
    Json.obj(
      "hasQti" -> i.hasQti,
      "hasPlayerDefinition" -> i.hasPlayerDefinition,
      "apiVersion" -> i.createdByApiVersion)
  }

  def getDraftFormat(id: String) = Action.async { implicit request =>

    def objectId = try{
      Some( new ObjectId(id))
    } catch {
      case _:Throwable => {
        logger.warn(s"Invalid object id: $id")
        None
      }
    }
    Future {
      {
        for {
          draftId <- objectId
          draft <- draftService.load(draftId)
        } yield {
          Ok(contentFormat(draft.src.data))
        }
      }.getOrElse(NotFound(""))
    }
  }

  def getItemFormat(id: String) = Action.async { implicit request =>
    Future {
      {
        for {
          vid <- VersionedId(id)
          item <- itemService.findOneById(vid)
        } yield {
          Ok(contentFormat(item))
        }
      }.getOrElse(NotFound(""))
    }
  }
}
