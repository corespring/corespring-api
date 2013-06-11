package api.v1

import api.v1.files.ItemFiles
import com.mongodb.casbah.Imports._
import com.typesafe.config.ConfigFactory
import controllers.auth.ApiRequest
import controllers.auth.{Permission, BaseApi}
import controllers.{ConcreteS3Service, S3Service}
import models.item.service.{ItemServiceClient, ItemServiceImpl, ItemService}
import models.item.{Content, Item}
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.mvc.{Action, Result, AnyContent}
import scala.Some
import scalaz.Scalaz._
import scalaz._


trait NewItemApi extends BaseApi with ItemServiceClient with ItemFiles {


  def itemService: ItemService

  //TODO: Map detail to a subset of fields to render
  def get(id: ObjectId, version: Option[Int] = None, detail: Option[String] = Some("normal")) = ItemApiAction(id, Permission.Read) {
    request =>
      itemService.findOneByIdAndVersion(id, version)
        .map(i => Ok(Json.toJson(i)))
        .getOrElse(NotFound)
  }

  def getDetail(id: ObjectId, version: Option[Int] = None) = get(id, version, Some("detailed"))


  def update(id: ObjectId, createNewVersion: Boolean = false) = ValidatedItemApiAction(id, Permission.Write) {
    request =>
      for {
        json <- request.body.asJson.toSuccess("No json in request body")
        item <- json.asOpt[Item].toSuccess("Bad json format - can't parse")
        validatedItem <- validateItem(id, item).toSuccess("Invalid data")
        savedResult <- saveItem(validatedItem, createNewVersion).toSuccess("Error saving item")
      } yield savedResult
  }

  def cloneItem(id: ObjectId) = ValidatedItemApiAction(id, Permission.Write) {
    request =>
      for {
        item <- itemService.findOneById(id).toSuccess("Can't find item")
        cloned <- itemService.cloneItem(item).toSuccess("Error cloning")
        c <- if (cloneStoredFiles(item, cloned)) scalaz.Success(cloned) else Failure("Error cloning files")
      } yield c
  }

  /** Wrap ItemApiAction so that we handle a ApiRequest => Validation and we generate the json.
    */
  private def ValidatedItemApiAction(id: ObjectId, p: Permission)
                                    (block: ApiRequest[AnyContent] => Validation[String,
                                      Item]): Action[AnyContent] = {
    def handleValidation(request: ApiRequest[AnyContent]): Result = {
      block(request) match {
        case Success(i) => Ok(Json.toJson(i))
        case Failure(e) => BadRequest(Json.toJson(JsObject(Seq("error" -> JsString(e)))))
      }
    }
    ItemApiAction(id, p)(handleValidation)

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

    Some(
      item.copy(
        id = id,
        collectionId = if (item.collectionId.isEmpty) itemService.findOneById(id).map(_.collectionId).getOrElse("") else item.collectionId)
    )
  }
  private def saveItem(item: Item, createNewVersion: Boolean): Option[Item] = {
    itemService.save(item, createNewVersion)
    itemService.findOneById(item.id)
  }

}

object NewItemApi extends NewItemApi {
  def s3service: S3Service = ConcreteS3Service

  def itemService: ItemService = ItemServiceImpl

  def bucket: String = ConfigFactory.load().getString("AMAZON_ASSETS_BUCKET")
}
