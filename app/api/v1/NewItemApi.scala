package api.v1

import api.v1.files.ItemFiles
import com.mongodb.casbah.Imports._
import com.typesafe.config.ConfigFactory
import controllers.auth.ApiRequest
import controllers.auth.{Permission, BaseApi}
import controllers.{ConcreteS3Service, S3Service}
import models.item.service.{ItemServiceClient, ItemServiceImpl, ItemService}
import models.item.Item
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.mvc.{Action, Result, AnyContent}
import scala.Some
import scalaz.Scalaz._
import scalaz._
import org.corespring.platform.data.mongo.models.VersionedId
import models.item.resource.StoredFile


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
        dbitem <- itemService.findOneById(id).toSuccess("no item found for the given id")
        validatedItem <- validateItem(dbitem, item).toSuccess("Invalid data")
        savedResult <- saveItem(validatedItem, dbitem.published).toSuccess("Error saving item")
      } yield savedResult
  }

  def cloneItem(id: VersionedId[ObjectId]) = ValidatedItemApiAction(id, Permission.Write) {
    request =>
      for {
        item <- itemService.findOneById(id).toSuccess("Can't find item")
        cloned <- itemService.cloneItem(item).toSuccess("Error cloning")
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
        if (models.item.Content.isAuthorized(request.ctx.organization, id, p)) {
          block(request)
        } else {
          Forbidden("Access forbidden")
        }
    }

  private def validateItem(dbitem:Item, item: Item): Option[Item] = {
    val itemCopy = item.copy(
      id = dbitem.id,
      collectionId = if (item.collectionId.isEmpty) dbitem.collectionId else item.collectionId
    )
    validateStoredFiles(dbitem,item)
    Some(itemCopy)
  }

  /**
   * add storage keys to item before update
   * @param dbitem
   * @param item
   */
  private def validateStoredFiles(dbitem:Item, item:Item) = {
    val itemsf:Seq[StoredFile] =
      item.data.map(r => r.files.filter(_.isInstanceOf[StoredFile]).map(_.asInstanceOf[StoredFile])).getOrElse(Seq()) ++
      item.supportingMaterials.map(r => r.files.filter(_.isInstanceOf[StoredFile]).map(_.asInstanceOf[StoredFile])).flatten
    val dbitemsf:Seq[StoredFile] =
      dbitem.data.map(r => r.files.filter(_.isInstanceOf[StoredFile]).map(_.asInstanceOf[StoredFile])).getOrElse(Seq()) ++
      dbitem.supportingMaterials.map(r => r.files.filter(_.isInstanceOf[StoredFile]).map(_.asInstanceOf[StoredFile])).flatten
    itemsf.foreach(sf => {
      dbitemsf.find(_.name == sf.name) match {
        case Some(dbsf) => sf.storageKey = dbsf.storageKey
        case None => Logger.warn("validateStoredFiles: no db storage key found")
      }
    })
  }
  private def saveItem(item: Item, createNewVersion: Boolean): Option[Item] = {
    itemService.save(item, createNewVersion)
    //Note: we remove the version - so that the dao automatically returns the latest version
    itemService.findOneById(VersionedId(item.id.id))
  }

}

object NewItemApi extends NewItemApi {
  def s3service: S3Service = ConcreteS3Service

  def itemService:ItemService = ItemServiceImpl

  def bucket: String = ConfigFactory.load().getString("AMAZON_ASSETS_BUCKET")
}
