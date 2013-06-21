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
import org.corespring.platform.data.mongo.models.VersionedId


trait NewItemApi extends BaseApi with ItemServiceClient with ItemFiles {


  def itemService:ItemService

  //TODO: Map detail to a subset of fields to render
  def get(id: VersionedId[ObjectId], detail: Option[String] = Some("normal")) = ItemApiAction(id, Permission.Read) {
    request =>
      itemService.findOneById(id)
        .map(i => Ok(Json.toJson(i)))
        .getOrElse(NotFound)
  }

  def getDetail(id: VersionedId[ObjectId] ) = get(id, Some("detailed"))


  def update(id: VersionedId[ObjectId]) = ValidatedItemApiAction(id, Permission.Write) {
    request =>
      for {
        json <- request.body.asJson.toSuccess("No json in request body")
        item <- json.asOpt[Item].toSuccess("Bad json format - can't parse")
        validatedItem <- validateItem(id, item).toSuccess("Invalid data")
        savedResult <- saveItem(validatedItem._1, validatedItem._2).toSuccess("Error saving item")
      } yield savedResult
  }

  def cloneItem(id: VersionedId[ObjectId]) = ValidatedItemApiAction(id, Permission.Write) {
    request =>
      for {
        cloned <- itemService.cloneItem(id).toSuccess("Error cloning")
      } yield cloned
  }

  /** Wrap ItemApiAction so that we handle a ApiRequest => Validation and we generate the json.
    */
  private def ValidatedItemApiAction(id: VersionedId[ObjectId], p: Permission)
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

  private def ItemApiAction(id: VersionedId[ObjectId], p: Permission)
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
  /**
   * return validated item and whether or not the item is published
   * @param id
   * @param item
   * @return
   */
  private def validateItem(id: VersionedId[ObjectId], item: Item): Option[(Item,Boolean)] = {
    val dbitem = itemService.findOneById(id);
    val isPublished = dbitem.map(_.published).getOrElse(false)
    Some((
      item.copy(
        id = id,
        collectionId = if (item.collectionId.isEmpty) itemService.findOneById(id).map(_.collectionId).getOrElse("") else item.collectionId),
        isPublished
    ))
  }
  private def saveItem(item: Item, createNewVersion: Boolean): Option[Item] = {
    itemService.save(item, createNewVersion)
    itemService.findOneById(item.id)
  }

}

object NewItemApi extends NewItemApi {
  def s3service: S3Service = ConcreteS3Service

  def itemService:ItemService = ItemServiceImpl

  def bucket: String = ConfigFactory.load().getString("AMAZON_ASSETS_BUCKET")
}
