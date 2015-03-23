package org.corespring.v2.player.hooks

import com.mongodb.casbah.commons.MongoDBObject
import org.corespring.amazon.s3.S3Service
import org.corespring.container.client.controllers.{ AssetType, Assets }
import org.corespring.container.client.hooks.{ AssetHooks => ContainerAssetHooks }
import org.corespring.platform.core.models.item.resource.{ Resource, BaseFile, StoredFile }
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.{ LoadOrgAndOptions, ItemAuth }
import org.corespring.v2.auth.models.OrgAndOpts
import play.api.libs.json.Json
import play.api.mvc.Results.BadRequest
import play.api.mvc._
import play.api.mvc.Results.Status

import scala.concurrent.Future

trait AssetHooks extends ContainerAssetHooks with LoadOrgAndOptions {

  import scalaz.Scalaz._
  import scalaz._

  //def s3: S3Service

  def assets: Assets

  def bucket: String

  def itemService: ItemService

  def auth: ItemAuth[OrgAndOpts]

  import play.api.http.Status._

  override def deleteFile(itemId: String, file: String)(implicit header: RequestHeader): Future[Option[(Int, String)]] = {

    val out = for {
      identity <- getOrgAndOptions(header)
      item <- auth.loadForWrite(itemId)(identity)
    } yield item

    out match {
      case Failure(e) => Future(Some(UNAUTHORIZED -> e.message))
      case _ => assets.delete(AssetType.Item, itemId, file)

    }
  }

  private def canUpload(itemId: String)(rh: RequestHeader): Option[SimpleResult] = {
    val result = for {
      identity <- getOrgAndOptions(rh)
      item <- auth.loadForWrite(itemId)(identity)
    } yield item

    result match {
      case Failure(e) => Some(Status(e.statusCode)(e.message))
      case _ => None
    }
  }

  override def uploadAction(itemId: String, file: String)(block: (Request[Int]) => SimpleResult): Action[Int] =
    Action(assets.upload(AssetType.Item, itemId, s"data/$file", canUpload(itemId))) {
      request =>
        val result: Validation[String, Result] = for {
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
        }

    }
  /*{


    Action(s3.upload(bucket, s"$itemId/data/$file", canUpload(itemId))) {
      request =>


    }
  }*/
}
