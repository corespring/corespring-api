package org.corespring.v2player.integration.controllers.editor

import com.mongodb.casbah.commons.MongoDBObject
import org.corespring.amazon.s3.S3Service
import org.corespring.container.client.actions.DeleteAssetRequest
import org.corespring.container.client.actions.{ AssetActions => ContainerAssetActions }
import org.corespring.platform.core.models.item.resource.{ StoredFile, BaseFile }
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.actionBuilders.AuthenticatedItem
import play.api.libs.json.Json
import play.api.mvc.Results.BadRequest
import play.api.mvc._

trait AssetActions extends ContainerAssetActions[AnyContent] {

  import scalaz.Scalaz._
  import scalaz._

  def s3: S3Service

  def bucket: String

  def itemService: ItemService
  def authForItem: AuthenticatedItem

  private def authenticatedFailure(itemId: String)(rh: RequestHeader): Option[SimpleResult] = {
    authForItem.authenticationFailedResult(itemId, rh)
  }

  override def delete(itemId: String, file: String)(block: (DeleteAssetRequest[AnyContent]) => Result): Action[AnyContent] = Action {
    request =>

      authenticatedFailure(itemId)(request).map {
        errorResult =>
          errorResult
      }.getOrElse {
        val result = s3.delete(bucket, s"$itemId/data/$file")
        if (result.success) {
          block(DeleteAssetRequest(None, request))
        } else {
          BadRequest(Json.obj("error" -> result.msg))
        }
      }
  }

  override def upload(itemId: String, file: String)(block: (Request[Int]) => Result): Action[Int] = {
    Action(s3.upload(bucket, s"$itemId/data/$file", authenticatedFailure(itemId))) {
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
