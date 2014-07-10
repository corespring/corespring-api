package org.corespring.v2player.integration.hooks

import com.mongodb.casbah.commons.MongoDBObject
import org.corespring.amazon.s3.S3Service
import org.corespring.container.client.hooks.{ AssetHooks => ContainerAssetHooks }
import org.corespring.platform.core.models.item.resource.{ BaseFile, StoredFile }
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.ItemAuth
import play.api.libs.json.Json
import play.api.mvc.Results.BadRequest
import play.api.mvc._

import scala.concurrent.Future

trait AssetHooks extends ContainerAssetHooks {

  import scalaz.Scalaz._
  import scalaz._

  def s3: S3Service

  def bucket: String

  def itemService: ItemService

  def auth: ItemAuth

  import play.api.http.Status._

  override def delete(itemId: String, file: String)(implicit header: RequestHeader): Future[Option[(Int, String)]] = Future {
    auth.loadForWrite(itemId) match {
      case Failure(e) => Some(UNAUTHORIZED -> e)
      case _ => {
        val r = s3.delete(bucket, s"$itemId/data/$file")
        if (r.success) None else Some(BAD_REQUEST -> r.msg)
      }
    }
  }

  private def canUpload(itemId: String)(rh: RequestHeader): Option[SimpleResult] = {
    auth.loadForWrite(itemId)(rh) match {
      case Failure(e) => Some(BadRequest(e))
      case _ => None
    }
  }

  override def uploadAction(itemId: String, file: String)(block: (Request[Int]) => SimpleResult): Action[Int] = {
    Action(s3.upload(bucket, s"$itemId/data/$file", canUpload(itemId))) {
      request =>

        val result: Validation[String, Result] = for {
          vid <- VersionedId(itemId).toSuccess(s"invalid item id: $itemId")
          item <- itemService.findOneById(vid).toSuccess(s"can't find item with id: $vid")
          data <- item.data.toSuccess(s"item doesn't contain a 'data' property': $vid")
        } yield {

          val filename = grizzled.file.util.basename(file)
          val newFile = StoredFile(file, BaseFile.getContentType(filename), false, s"$itemId/data/$file")
          import org.corespring.platform.core.models.mongoContext.context
          val dbo = com.novus.salat.grater[StoredFile].asDBObject(newFile)

          itemService.collection.update(
            MongoDBObject("_id._id" -> vid.id),
            MongoDBObject("$addToSet" -> MongoDBObject("data.files" -> dbo)),
            false)
          block(request)
        }

        result match {
          case Success(r) => r
          case Failure(msg) => BadRequest(Json.obj("err" -> msg))
        }

    }
  }
}
