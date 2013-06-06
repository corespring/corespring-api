package api.v1

import api.v1.files.ItemFiles
import com.mongodb.casbah.Imports._
import com.typesafe.config.ConfigFactory
import controllers.auth.ApiRequest
import controllers.auth.{Permission, BaseApi}
import controllers.{ConcreteS3Service, S3Service}
import models.item.{Content, Item}
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.mvc.{Action, Result, AnyContent}
import scala.Some
import scalaz.Scalaz._
import scalaz._


trait NewItemApi extends BaseApi with ItemFiles {


  def get(id: ObjectId, version: Option[Int] = None) = ItemApiAction(id, Permission.Read) {
    request =>
      Item.findOneByIdAndVersion(id, version)
        .map(i => Ok(Json.toJson(i)))
        .getOrElse(NotFound)
  }


  def update(id: ObjectId, createNewVersion: Boolean = false) = ItemApiAction(id, Permission.Write) {
    request =>
      val result: Validation[String, Item] = for {
        json <- request.body.asJson.toSuccess("No json in request body")
        item <- json.asOpt[Item].toSuccess("Bad json format - can't parse")
        validatedItem <- validateItem(id, item).toSuccess("Invalid data")
        savedResult <- saveItem(validatedItem, createNewVersion).toSuccess("Error saving item")
      } yield savedResult

      result match {
        case Success(item) => Ok(Json.toJson(item))
        case Failure(e) => BadRequest(Json.toJson(JsObject(Seq("error" -> JsString(e)))))
      }
  }

  def cloneItem(id: ObjectId) = ItemApiAction(id, Permission.Write) {
    request =>
      val result: Validation[String, Item] = for {
        item <- Item.findOneById(id).toSuccess("Can't find item")
        cloned <- Item.cloneItem(item).toSuccess("Error cloning")
        c <- if (cloneStoredFiles(item, cloned)) scalaz.Success(cloned) else Failure("Error cloning files")
      } yield c

      result match {
        case Success(c) => Ok(Json.toJson(c))
        case Failure(e) => BadRequest(Json.toJson(JsObject(Seq("error" -> JsString(e)))))
      }
  }

  private def ItemApiAction(id: ObjectId, p: Permission)
                           (block: ApiRequest[AnyContent] => Result):
  Action[AnyContent] =
    ApiAction {
      request =>
        if (Content.isAuthorized(request.ctx.organization, id, p)) {
          block(request)
        } else {
          Forbidden
        }
    }


  //TODO: flesh out
  private def validateItem(id: ObjectId, item: Item): Option[Item] = {
    Some(item.copy(id = id))
  }

  private def saveItem(item: Item, createNewVersion: Boolean): Option[Item] = {
    Item.save(item, createNewVersion)
    Item.findOneById(item.id)
  }

}

object NewItemApi extends NewItemApi {
  def s3service: S3Service = ConcreteS3Service

  def bucket: String = ConfigFactory.load().getString("AMAZON_ASSETS_BUCKET")
}
