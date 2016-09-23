package org.corespring.api.v1

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSONParseException
import salat.Context
import salat.dao.SalatInsertError
import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.models.auth.Permission
import org.corespring.models.item.{ Item, TaskInfo }
import org.corespring.models.json.JsonFormatting
import org.corespring.platform.core.controllers.auth.{ ApiRequest, OAuthProvider }
import org.corespring.platform.core.models.search.SearchFields
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.corespring.services.metadata.MetadataSetService
import org.corespring.services.{ OrgCollectionService, OrganizationService }
import org.corespring.v2.sessiondb.SessionServices
import org.corespring.web.api.v1.errors.ApiError
import play.api.libs.json.Json._
import play.api.libs.json.{ JsNumber, JsObject, JsString, _ }
import play.api.mvc.{ Action, AnyContent, Result }

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.Scalaz._
import scalaz.{ Failure, Success, _ }

/**
 * Items API
 * //TODO: Look at ways of tidying this class up, there are too many mixed activities going on.
 */
class ItemApi(
  v2ItemApi: org.corespring.v2.api.ItemApi,
  s3service: S3Service,
  service: ItemService,
  salatService: SalatContentService[Item, _],
  metadataSetService: MetadataSetService,
  orgService: OrganizationService,
  orgCollectionService: OrgCollectionService,
  sessionServices: SessionServices,
  itemTransformer: ItemTransformer,
  jsonFormatting: JsonFormatting,
  itemApiItemValidation: ItemApiItemValidation,
  v1ExecutionContext: V1ApiExecutionContext,
  val oAuthProvider: OAuthProvider,
  override implicit val context: Context)
  extends ContentApi[Item](
    salatService,
    orgService,
    orgCollectionService,
    context,
    ItemView.Writes) {

  import jsonFormatting.item

  def listWithOrg(orgId: ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) = ApiAction {
    implicit request =>
      if (orgService.getTree(request.ctx.orgId).exists(_.id == orgId)) {
        val collections = getCollectionIds(orgId, Permission.Read)
        val jsonBuilder = if (c == "true") countOnlyJson _ else contentOnlyJson _
        contentList(q, f, sk, l, sort, collections, true, jsonBuilder) match {
          case Left(apiError) => BadRequest(toJson(apiError))
          case Right(json) => Ok(json)
        }
      } else Forbidden(toJson(ApiError.UnauthorizedOrganization))
  }

  def listWithColl(collId: ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) = AsyncApiAction {
    implicit request =>
      implicit val ec = v1ExecutionContext.context
      Future {
        if (orgCollectionService.isAuthorized(request.ctx.orgId, collId, Permission.Read)) {
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
  }

  def update(id: VersionedId[ObjectId]) = ValidatedItemApiAction(id, Permission.Write) {
    request =>
      for {
        json <- request.body.asJson.toSuccess("No json in request body")
        item <- json.asOpt[Item].toSuccess("Bad json format - can't parse")
        dbItem <- service.findOneById(id).toSuccess("no item found for the given id")
        validatedItem <- itemApiItemValidation.validateItem(dbItem, item)
        savedResult <- saveItem(validatedItem, dbItem.published && (sessionCount(dbItem) > 0))
        withV2DataItem <- Success(itemTransformer.updateV2Json(savedResult))
      } yield {
        withV2DataItem
      }
  }

  private def sessionCount(item: Item): Long = {
    sessionServices.main.sessionCount(item.id)
  }

  def countSessions(id: VersionedId[ObjectId]) = ApiAction {
    request =>
      val c = for {
        item <- service.findOneById(id).toSuccess("Can't find item")
      } yield sessionCount(item)
      c match {
        case Success(_) => Ok(JsObject(Seq("sessionCount" -> JsNumber(c.toOption.get))))
        case _ => BadRequest
      }
  }

  def cloneItem(id: VersionedId[ObjectId]) = ValidatedItemApiAction(id, Permission.Write) {
    request =>
      for {
        item <- service.findOneById(id).toSuccess("Can't find item")
        cloned <- service.clone(item)
      } yield cloned
  }

  /**
   * Note: we remove the version - so that the dao automatically returns the latest version
   */
  private def saveItem(item: Item, createNewVersion: Boolean): Validation[String, Item] = {
    for {
      newItem <- service.save(item, createNewVersion).leftMap(_.message)
      dbItem <- service.findOneById(item.id.copy(version = None)).toSuccess("Error loading item")
    } yield dbItem
  }

  def get(id: VersionedId[ObjectId], detail: Option[String] = Some("normal")) = ItemApiAction(id, Permission.Read) {
    request =>

      val fields = detail.map { d =>
        if (d == "detailed") getFieldsDbo(false, request.ctx.isLoggedInUser, Seq(Item.Keys.data)) else getFieldsDbo(true, request.ctx.isLoggedInUser)
      }.getOrElse(getFieldsDbo(true, request.ctx.isLoggedInUser))

      logger.debug("[ItemApi.get] fields: " + fields)

      import com.mongodb.casbah.Imports._

      service.findFieldsById(id, fields)
        .map(dbo => salat.grater[Item].asObject[Imports.DBObject](dbo))
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
        if (service.isAuthorized(request.ctx.orgId, id, p).isSuccess) {
          block(request)
        } else {
          val orgName = request.ctx.org.name
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
  def delete(id: VersionedId[ObjectId]) = v2ItemApi.delete(id.toString)

  private def itemFromJson(json: JsValue): Item = {
    json.asOpt[Item].getOrElse {
      throw new Exception("TODO 2.1.1 upgrade- handle this correctly")
    }
  }

  private def isCollectionAuthorized(orgId: ObjectId, collectionId: String, p: Permission): Boolean = {
    val ids = getCollectionIds(orgId, p)
    logger.debug(s"function=isCollectionAuthorized, orgId=$orgId, ids=$ids, collectionId=$collectionId")
    ids.exists(_.toString == id)
  }

  def create = ApiAction {
    request =>
      request.body.asJson match {
        case Some(json) => {
          try {
            if ((json \ "id").asOpt[String].isDefined) {
              BadRequest(toJson(ApiError.IdNotNeeded))
            } else {

              def withCollectionId(i: Item): Item = if (i.collectionId.isEmpty && request.ctx.permission.has(Permission.Write)) {
                orgCollectionService.getDefaultCollection(request.ctx.orgId).toEither match {
                  case Right(default) => {
                    i.copy(collectionId = default.id.toString)
                  }
                  case Left(error) => i
                }
              } else i

              val i = withCollectionId(itemFromJson(json))

              if (isCollectionAuthorized(request.ctx.orgId, i.collectionId, Permission.Write)) {
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
                  service.save(item.copy(taskInfo = Some(taskInfo)), false)
                  val json = jsonFormatting.formatTaskInfo.extendedAsJson(taskInfo.extended)
                  Ok(json)
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
                  service.save(item.copy(taskInfo = Some(taskInfo)), false)
                  val json = jsonFormatting.formatTaskInfo.extendedAsJson(taskInfo.extended)
                  Ok(json)
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

  def forceTransform(id: VersionedId[ObjectId]) = Action {
    val version = latestVersion(id)
    itemTransformer.updateV2Json(VersionedId[ObjectId](id = id.id, version = Some(version.toLong))) match {
      case Some(item) => Ok("Item transformed")
      case None => InternalServerError("There was a problem transforming the item")
    }
  }

}

