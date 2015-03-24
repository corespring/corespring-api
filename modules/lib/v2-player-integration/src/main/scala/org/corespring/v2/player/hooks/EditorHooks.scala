package org.corespring.v2.player.hooks

import org.corespring.amazon.s3.S3Service
import org.corespring.v2.auth.models.OrgAndOpts

import scala.concurrent.Future

import org.corespring.container.client.hooks.{ EditorHooks => ContainerEditorHooks, UploadResult, PlayerData }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.v2.auth.{ LoadOrgAndOptions, ItemAuth }
import org.corespring.v2.log.V2LoggerFactory
import play.api.libs.json.JsValue
import play.api.mvc._
import scalaz.{ Failure, Success }

trait ItemDraftAuth[A] {
  def canWrite(id: String)(identity: A): Boolean
}
trait EditorHooks extends ContainerEditorHooks with LoadOrgAndOptions {

  import play.api.http.Status._

  def itemService: ItemService

  private lazy val logger = V2LoggerFactory.getLogger("EditorHooks")

  import scalaz._

  def playS3: S3Service

  def bucket: String

  def auth: ItemDraftAuth[OrgAndOpts]

  override def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult = {
    playS3.download(bucket, mkPath(id, path))
  }

  override def deleteFile(id: String, file: String)(implicit header: RequestHeader): Future[Option[(Int, String)]] = {

    val out = for {
      identity <- getOrgAndOptions(header)
      canWrite <- auth.canWrite(id)(identity)
    } yield canWrite

    out match {
      case Failure(e) => Future(Some(UNAUTHORIZED -> e.message))
      case _ => {
        Future {
          val response = playS3.delete(bucket, mkPath(id, file))
          if (response.success) {
            None
          } else {
            Some(BAD_REQUEST -> response.msg)
          }
        }
      }
    }
  }

  def mkPath(parts: String*) = ("item-drafts" :+ parts).mkString("/")

  override def upload(draftId: String, file: String)(predicate: (RequestHeader) => Option[SimpleResult]): BodyParser[Future[UploadResult]] = {

    def canUpload(rh: RequestHeader): Option[SimpleResult] = {
      val result = for {
        identity <- getOrgAndOptions(rh)
        canWrite <- auth.canWrite(draftId)(identity)
      } yield canWrite

      result match {
        case Success(true) => None
        case Success(false) => Some(Results.Status(UNAUTHORIZED)("You can't edit this draft"))
        case Failure(e) => Some(Results.Status(e.statusCode)(e.message))
      }
    }

    playS3.s3Object(bucket, mkPath(draftId, file))(canUpload).map { f =>
      f.map { s3Obj =>
        UploadResult(s3Obj.getKey)
      }
    }
  }
  /*val result: Validation[String, Result] = for {
    vid <- VersionedId(itemId).toSuccess(s"invalid item id: $itemId")
    item <- itemService.findOneById(vid).toSuccess(s"can't find item with id: $vid")
  } yield {

    val filename = grizzled.file.util.basename(file)
    val newFile = StoredFile(file, BaseFile.getContentType(filename), false, s"$itemId/data/$file")
    import org.corespring.platform.core.models.mongoContext.context

    item.data.map { d =>

      val dbo = com.novus.salat.grater[StoredFile].asDBObject(newFile)
      itemService.collection.update(
        MongoDBObject("_id._id" -> vid.id),
        MongoDBObject("$addToSet" -> MongoDBObject("data.files" -> dbo)),
        false)

    }.getOrElse {

      val resource = Resource(None, "data", files = Seq(newFile))
      val resourceDbo = com.novus.salat.grater[Resource].asDBObject(resource)

      itemService.collection.update(
        MongoDBObject("_id._id" -> vid.id),
        MongoDBObject("$set" -> MongoDBObject("data" -> resourceDbo)),
        false)
    }
    block(request)
  }

  result match {
    case Success(r) => r
    case Failure(msg) => BadRequest(Json.obj("err" -> msg))
  }*/

  /**
   * Attempt to load the item for write. If that fails but reading is possible redirect to the catalog view
   * @param itemId
   * @param header
   * @return
   */

  override def loadItem(itemId: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {

    logger.trace(s"load item: $itemId")

    val result = for {
      identity <- getOrgAndOptions(header)
      item <- auth.canWrite(itemId)(identity)
    } yield item

    result match {
      case Success(item) => Right(transform(item))
      case Failure(e) => {
        logger.trace(s"can't load item: $itemId for writing - try to load for read and if successful return a SEE_OTHER")

        val readableResult = for {
          identity <- getOrgAndOptions(header)
          readableItem <- auth.loadForRead(itemId)(identity)
        } yield readableItem

        readableResult match {
          case Success(item) => Left(SEE_OTHER -> org.corespring.container.client.controllers.apps.routes.Catalog.load(itemId).url)
          case Failure(e) => Left(UNAUTHORIZED -> e.message)
        }
      }
    }
  }

}
