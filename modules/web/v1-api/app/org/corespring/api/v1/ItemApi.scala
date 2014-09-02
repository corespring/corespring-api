package org.corespring.api.v1

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSONParseException
import com.novus.salat.dao.SalatInsertError
import org.bson.types.ObjectId
import org.corespring.api.v1.errors.ApiError
import org.corespring.assets.{ CorespringS3Service, CorespringS3ServiceExtended }
import org.corespring.common.log.PackageLogging
import org.corespring.platform.core.controllers.auth.ApiRequest
import org.corespring.platform.core.models._
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item._
import org.corespring.platform.core.models.item.resource.StoredFile
import org.corespring.platform.core.models.json.ItemView
import org.corespring.platform.core.models.search.SearchFields
import org.corespring.platform.core.services.item.{ ItemService, ItemServiceWired }
import org.corespring.platform.core.services.metadata.{ MetadataSetService, MetadataSetServiceImpl }
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import play.api.{ Configuration, Play }
import play.api.libs.json.Json._
import play.api.libs.json.{ JsNumber, JsObject, JsString, _ }
import play.api.mvc.{ Action, AnyContent, Result }

import scalaz.Scalaz._
import scalaz.{ Failure, Success, _ }

/**
 * Items API
 * //TODO: Look at ways of tidying this class up, there are too many mixed activities going on.
 */
class ItemApi(s3service: CorespringS3Service, service: ItemService, metadataSetService: MetadataSetService)
  extends ContentApi[Item](service)(ItemView.Writes) with PackageLogging {

  import org.corespring.platform.core.models.item.Item.Keys._
  import org.corespring.platform.core.models.mongoContext.context

  val itemTransformer = new ItemTransformer {
    override def cache: ItemTransformationCache = PlayItemTransformationCache
    override def itemService: ItemService = service
    override def configuration: Configuration = Play.current.configuration
  }

  def listWithOrg(orgId: ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) = ApiAction {
    implicit request =>
      if (Organization.getTree(request.ctx.organization).exists(_.id == orgId)) {
        val collections = ContentCollection.getCollectionIds(orgId, Permission.Read)
        val jsonBuilder = if (c == "true") countOnlyJson _ else contentOnlyJson _
        contentList(q, f, sk, l, sort, collections, true, jsonBuilder) match {
          case Left(apiError) => BadRequest(toJson(apiError))
          case Right(json) => Ok(json)
        }
      } else Forbidden(toJson(ApiError.UnauthorizedOrganization))
  }

  def listWithColl(collId: ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) = ApiAction {
    implicit request =>
      if (ContentCollection.isAuthorized(request.ctx.organization, collId, Permission.Read)) {
        val jsBuilder = if (c == "true") countOnlyJson _ else contentOnlyJson _
        contentList(q, f, sk, l, sort, Seq(collId), true, jsBuilder) match {
          case Left(apiError) =>
            BadRequest(toJson(apiError))
          case Right(json) => Ok(json)
        }
      } else {
        Unauthorized(toJson(ApiError.UnauthorizedOrganization))
      }
  }

  def update(id: VersionedId[ObjectId]) = ValidatedItemApiAction(id, Permission.Write) {
    request =>
      for {
        json <- request.body.asJson.toSuccess("No json in request body")
        item <- json.asOpt[Item].toSuccess("Bad json format - can't parse")
        dbitem <- service.findOneById(id).toSuccess("no item found for the given id")
        validatedItem <- validateItem(dbitem, item).toSuccess("Invalid data")
        savedResult <- saveItem(validatedItem, dbitem.published && (service.sessionCount(dbitem) > 0)).toSuccess("Error saving item")
        withV2DataItem <- itemTransformer.updateV2Json(savedResult).toSuccess("Error generating item v2 JSON")
      } yield {
        PlayItemTransformationCache.removeCachedTransformation(item)
        withV2DataItem
      }
  }

  def countSessions(id: VersionedId[ObjectId]) = ApiAction {
    request =>
      val c = for {
        item <- service.findOneById(id).toSuccess("Can't find item")
      } yield service.sessionCount(item)
      c match {
        case Success(_) => Ok(JsObject(Seq("sessionCount" -> JsNumber(c.toOption.get))))
        case _ => BadRequest
      }
  }

  def cloneItem(id: VersionedId[ObjectId]) = ValidatedItemApiAction(id, Permission.Write) {
    request =>
      for {
        item <- service.findOneById(id).toSuccess("Can't find item")
        cloned <- service.clone(item).toSuccess("Error cloning")
      } yield cloned
  }

  private def validateItem(dbItem: Item, item: Item): Option[Item] = {
    val itemCopy = item.copy(
      id = dbItem.id,
      collectionId = if (item.collectionId.isEmpty) dbItem.collectionId else item.collectionId,
      taskInfo = item.taskInfo.map(_.copy(extended = dbItem.taskInfo.getOrElse(TaskInfo()).extended)) //
      )
    addStorageKeysToItem(dbItem, item)
    Some(itemCopy)
  }

  /**
   * TODO: Remove code duplication here..
   * add storage keys to item before update
   * @param dbItem
   * @param item
   */
  private def addStorageKeysToItem(dbItem: Item, item: Item) = {
    val itemsf: Seq[StoredFile] =
      item.data.map(r => r.files.filter(_.isInstanceOf[StoredFile]).map(_.asInstanceOf[StoredFile])).getOrElse(Seq()) ++
        item.supportingMaterials.map(r => r.files.filter(_.isInstanceOf[StoredFile]).map(_.asInstanceOf[StoredFile])).flatten
    val dbitemsf: Seq[StoredFile] =
      dbItem.data.map(r => r.files.filter(_.isInstanceOf[StoredFile]).map(_.asInstanceOf[StoredFile])).getOrElse(Seq()) ++
        dbItem.supportingMaterials.map(r => r.files.filter(_.isInstanceOf[StoredFile]).map(_.asInstanceOf[StoredFile])).flatten
    itemsf.foreach(sf => {
      dbitemsf.find(_.name == sf.name) match {
        case Some(dbsf) => sf.storageKey = dbsf.storageKey
        case None => logger.warn("addStorageKeysToItem: no db storage key found")
      }
    })
  }

  /**
   * Note: we remove the version - so that the dao automatically returns the latest version
   */
  private def saveItem(item: Item, createNewVersion: Boolean): Option[Item] = {
    service.save(item, createNewVersion)
    val vid: VersionedId[ObjectId] = item.id.copy(version = None)
    service.findOneById(vid)
  }

  def get(id: VersionedId[ObjectId], detail: Option[String] = Some("normal")) = ItemApiAction(id, Permission.Read) {
    request =>

      val fields = detail.map { d =>
        if (d == "detailed") getFieldsDbo(false, request.ctx.isLoggedIn, Seq(data)) else getFieldsDbo(true, request.ctx.isLoggedIn)
      }.getOrElse(getFieldsDbo(true, request.ctx.isLoggedIn))

      logger.debug("[ItemApi.get] fields: " + fields)

      import com.mongodb.casbah.Imports._

      service.findFieldsById(id, fields)
        .map(dbo => com.novus.salat.grater[Item].asObject[Imports.DBObject](dbo))
        .map(i => Ok(Json.toJson(i).as[JsObject] + ("latest" -> JsNumber(latestVersion(id)))))
        .getOrElse(NotFound)
  }

  private def latestVersion(id: VersionedId[ObjectId]): Int = (service.findOneById(id.copy(version = None)) match {
    case Some(item) => item.id.version
    case None => id.version
  }).getOrElse(0).toString.toDouble.toInt // not sure why we need the toString.toInt, but get ClassCastException otherwise

  def getDetail(id: VersionedId[ObjectId]) = get(id, Some("detailed"))

  /**
   * Wrap ItemApiAction so that we handle a ApiRequest => Validation and we generate the json.
   */
  private def ValidatedItemApiAction(id: VersionedId[ObjectId], p: Permission)(block: ApiRequest[AnyContent] => Validation[String, Item]): Action[AnyContent] = {
    def handleValidation(request: ApiRequest[AnyContent]): Result = {
      block(request) match {
        case Success(i) => Ok(Json.toJson(i))
        case Failure(e) => BadRequest(Json.toJson(JsObject(Seq("error" -> JsString(e)))))
      }
    }
    ItemApiAction(id, p)(handleValidation)

  }

  private def ItemApiAction(id: VersionedId[ObjectId], p: Permission)(block: ApiRequest[AnyContent] => Result): Action[AnyContent] =
    ApiAction {
      request =>
        if (Content.isAuthorized(request.ctx.organization, id, p)) {
          block(request)
        } else {
          val orgName = Organization.findOneById(request.ctx.organization).map(_.name).getOrElse("unknown org")
          val message = "Access forbidden for org: " + orgName
          Forbidden(JsObject(Seq("message" -> JsString(message))))
        }
    }

  /**
   * A wrapper around the dbfields logic
   * TODO: tidy up dbfields stuff
   */
  private def getFieldsDbo(include: Boolean, isLoggedIn: Boolean, otherFields: Seq[String] = Seq()): DBObject = {
    val searchFields = SearchFields(method = if (include) 1 else 0)
    cleanDbFields(searchFields, isLoggedIn, otherFields)
    searchFields.dbfields
  }

  /**
   * Deletes the item matching the id specified
   */
  def delete(id: VersionedId[ObjectId]) = ApiAction {
    request =>
      service.findFieldsById(id, MongoDBObject(collectionId -> 1)) match {
        case Some(dbObject) => dbObject.get(collectionId) match {
          case collId: String => if (Content.isCollectionAuthorized(request.ctx.organization, Some(collId), Permission.Write)) {
            Content.moveToArchive(id) match {
              case Right(_) => Ok(com.mongodb.util.JSON.serialize(dbObject))
              case Left(error) => InternalServerError(toJson(ApiError.Item.Delete(error.clientOutput)))
            }
          } else {
            Forbidden
          }
          case _ => Forbidden
        }
        case _ => NotFound
      }
  }

  private def itemFromJson(json: JsValue): Item = {
    json.asOpt[Item].getOrElse {
      throw new Exception("TODO 2.1.1 upgrade- handle this correctly")
    }
  }

  def create = ApiAction {
    request =>
      request.body.asJson match {
        case Some(json) => {
          try {
            if ((json \ "id").asOpt[String].isDefined) {
              BadRequest(toJson(ApiError.IdNotNeeded))
            } else {
              val i = itemFromJson(json)
              if (i.collectionId.isEmpty && request.ctx.permission.has(Permission.Write)) {
                Organization.getDefaultCollection(request.ctx.organization) match {
                  case Right(default) => {
                    i.collectionId = Option(default.id.toString)
                    service.insert(i) match {
                      case Some(_) => Ok(toJson(i))
                      case None => InternalServerError(toJson(ApiError.CantSave))
                    }
                  }
                  case Left(error) => InternalServerError(toJson(ApiError.CantSave(error.clientOutput)))
                }
              } else if (Content.isCollectionAuthorized(request.ctx.organization, i.collectionId, Permission.Write)) {
                service.insert(i) match {
                  case Some(_) => Ok(toJson(i))
                  case None => InternalServerError(toJson(ApiError.CantSave))
                }
              } else {
                Unauthorized(toJson(ApiError.CollectionUnauthorized))
              }
            }
          } catch {
            case parseEx: JSONParseException => BadRequest(toJson(ApiError.JsonExpected))
            case e: SalatInsertError => InternalServerError(toJson(ApiError.CantSave))
          }
        }
        case _ => BadRequest(toJson(ApiError.JsonExpected))
      }
  }
  def updateMetadata(id: VersionedId[ObjectId], property: String) = ApiAction { request =>
    service.findOneById(id) match {
      case Some(item) => {
        val splitprops = property.split("\\.")
        if (splitprops.length > 1) { //attempting update a property within a metadata set
          val metadataKey: String = splitprops(0)
          val key = splitprops(1)
          //since there is a period delimeter, we assume that the body is meant to be a single value for the given key. therefore we serialize if there is json
          val value: Option[String] = request.body.asText match {
            case Some(v) => Some(v)
            case None => request.body.asJson match {
              case Some(v) => Some(v.toString())
              case None => None
            }
          }
          if (value.isDefined) {
            metadataSetService.findByKey(metadataKey) match {
              case Some(ms) => { //check to make sure the given property matches the schema, if there is a a schema
                if (ms.schema.isEmpty || ms.schema.find(sm => sm.key == key).isDefined) {
                  //update metadata
                  val taskInfo: TaskInfo = item.taskInfo.getOrElse(TaskInfo())
                  taskInfo.extended.find(_._1 == metadataKey) match {
                    case Some(m) => m._2.put(key, value.get)
                    case None => taskInfo.extended.put(metadataKey, new BasicDBObject(key, value.get))
                  }
                  item.taskInfo = Some(taskInfo)
                  service.save(item, false)
                  Ok(TaskInfo.extendedAsJson(taskInfo.extended))
                } else BadRequest(Json.toJson(ApiError.MetadataNotFound(Some("you are attempting to add a property that does not match the set schema"))))
              }
              case None => BadRequest(Json.toJson(ApiError.MetadataNotFound(Some("specified set was not found"))))
            }
          } else {
            BadRequest(Json.toJson(ApiError.BodyNotFound))
          }
        } else { //attempting to update an entire metadata set
          //since property does not have a period delimeter, we assume property is the metadata key
          // and the user is attempting to update an entire metadata set within the item

          val incomingData = for (json <- request.body.asJson; map <- json.asOpt[scala.collection.immutable.Map[String, String]]) yield map

          incomingData.map { m =>

            import scala.collection.JavaConversions._
            val mutableMap = collection.mutable.Map(m.toSeq: _*)

            metadataSetService.findByKey(property) match {
              case Some(ms) => { //check to make sure the given properties matches the schema, if there is a a schema
                if (ms.schema.isEmpty || ms.schema.forall(sm => mutableMap.contains(sm.key))) {
                  val taskInfo: TaskInfo = item.taskInfo.getOrElse(TaskInfo())
                  taskInfo.extended.put(property, new BasicDBObject(mutableMap))
                  item.taskInfo = Some(taskInfo)
                  service.save(item, false)
                  Ok(TaskInfo.extendedAsJson(taskInfo.extended))
                } else BadRequest(Json.toJson(ApiError.MetadataNotFound(Some("you are attempting to add a property that does not match the set schema"))))
              }
              case None => BadRequest(Json.toJson(ApiError.MetadataNotFound(Some("specified set was not found"))))
            }
          }.getOrElse(BadRequest(Json.toJson(ApiError.BodyNotFound)))
        }
      }
      case None => NotFound(Json.toJson(ApiError.IdNotFound))
    }
  }
  def getMetadata(id: VersionedId[ObjectId], property: String) = ApiAction { request =>
    service.findOneById(id) match {
      case Some(item) => {
        val splitprops = property.split("\\.")
        if (splitprops.length > 1) {
          val metadataKey: String = splitprops(0)
          val key = splitprops(1)
          item.taskInfo.flatMap(_.extended.find(_._1 == metadataKey).map(_._2)) match {
            case Some(metadataProps) => {
              metadataProps.find(_._1 == key).map(_._2) match {
                case Some(value) => Ok(Json.obj(key -> value.toString))
                case None => BadRequest(Json.toJson(ApiError.MetadataNotFound(Some("could not find metadata property " + key + " in the set " + metadataKey))))
              }
            }
            case None => BadRequest(Json.toJson(ApiError.MetadataNotFound(Some("could not find metadata set key in item"))))
          }
        } else {
          item.taskInfo.flatMap(_.extended.find(_._1 == property).map(_._2)) match {
            case Some(metadataProps) => {
              Ok(Json.obj(metadataProps.map(prop => prop._1 -> toJsFieldJsValueWrapper(prop._2.toString)).toSeq: _*))
            }
            case None => BadRequest(Json.toJson(ApiError.MetadataNotFound(Some("could not find metadata set key in item"))))
          }
        }
      }
      case None => NotFound(Json.toJson(ApiError.IdNotFound))
    }
  }

  def contentType = Item.contentType

  override def cleanDbFields(searchFields: SearchFields, isLoggedIn: Boolean, dbExtraFields: Seq[String] = dbSummaryFields, jsExtraFields: Seq[String] = jsonSummaryFields) = {

    val mongoDbFields: MongoDBObject = searchFields.dbfields

    logger.trace(s"[cleanDbFields] logged in: ${isLoggedIn}, db fields empty: ${mongoDbFields.isEmpty}")

    if (!isLoggedIn && mongoDbFields.isEmpty) {
      dbExtraFields.foreach(extraField =>
        searchFields.dbfields = searchFields.dbfields ++ MongoDBObject(extraField -> searchFields.method))
      jsExtraFields.foreach(extraField =>
        searchFields.jsfields = searchFields.jsfields :+ extraField)
    }
    if (searchFields.method == 1 && searchFields.dbfields.nonEmpty) {
      searchFields.dbfields = searchFields.dbfields
    }
    logger.trace(s"[cleanDbFields] search fields now: ${searchFields.toString}")
  }

}
object dependencies {

  val metadataSetService: MetadataSetServiceImpl = new MetadataSetServiceImpl {
    def orgService: OrganizationService = new OrganizationImpl {
      def metadataSetService: MetadataSetServiceImpl = dependencies.metadataSetService
    }
  }
}

object ItemApi extends ItemApi(CorespringS3ServiceExtended, ItemServiceWired, dependencies.metadataSetService)

