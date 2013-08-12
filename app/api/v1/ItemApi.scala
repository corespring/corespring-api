package api.v1

import api.ApiError
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSONParseException
import com.novus.salat.dao.SalatInsertError
import com.novus.salat.dao.SalatMongoCursor
import controllers.auth.ApiRequest
import controllers.auth.BaseApi
import org.corespring.common.log.PackageLogging
import org.corespring.platform.core.models._
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.error.InternalError
import org.corespring.platform.core.models.item.resource.StoredFile
import org.corespring.platform.core.models.item.{TaskInfo, Item, Alignments, Content}
import org.corespring.platform.core.models.json.ItemView
import org.corespring.platform.core.models.mongoContext.context
import org.corespring.platform.core.models.search.ItemSearch
import org.corespring.platform.core.models.search.SearchCancelled
import org.corespring.platform.core.models.search.SearchFields
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json.Json._
import play.api.libs.json._
import play.api.mvc.{Result, Action, AnyContent}
import scala.Some
import scalaz.Failure
import scalaz.Scalaz._
import scalaz.Success
import scalaz.Validation
import org.corespring.assets.{CorespringS3ServiceImpl, CorespringS3Service}
import org.corespring.platform.core.services.item.{ItemServiceImpl, ItemService}

/**
 * Items API
 * //TODO: Look at ways of tidying this class up, there are too many mixed activities going on.
 */
class ItemApi(s3service: CorespringS3Service, service :ItemService) extends BaseApi with PackageLogging {

  import Item.Keys._

  val dbSummaryFields = Seq(collectionId, taskInfo, otherAlignments, standards, contributorDetails, published)
  val jsonSummaryFields: Seq[String] = Seq("id",
    collectionId,
    TaskInfo.Keys.gradeLevel,
    TaskInfo.Keys.itemType,
    Alignments.Keys.keySkills,
    primarySubject,
    relatedSubject,
    standards,
    author,
    TaskInfo.Keys.title,
    published)

  /**
   * List query implementation for Items
   */
  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) = ApiAction {
    implicit request =>
      val collections = ContentCollection.getCollectionIds(request.ctx.organization, Permission.Read)

      val jsonBuilder = if(c == "true") countOnlyJson _ else itemOnlyJson _
      itemList(q, f, sk, l, sort, collections, true, jsonBuilder) match {
        case Left(apiError) => BadRequest(toJson(apiError))
        case Right(json) => Ok(json)
      }
  }

  def listAndCount(q: Option[String], f: Option[String], sk: Int, l: Int, sort: Option[String]) = ApiAction {
    implicit request =>
      val collections = ContentCollection.getCollectionIds(request.ctx.organization, Permission.Read)

      itemList(q, f, sk, l, sort, collections, true, countAndListJson) match {
        case Left(apiError) => BadRequest(toJson(apiError))
        case Right(json) => Ok(json)
      }
  }

  def countAndListJson(count: Int, cursor: SalatMongoCursor[Item], searchFields: SearchFields, current: Boolean = true): JsValue = {
    val itemViews: Seq[ItemView] = cursor.toList.map(ItemView(_, Some(searchFields)))
    JsObject(Seq("count" -> JsNumber(count), "data" -> toJson(itemViews)))
  }

  def countOnlyJson(count: Int, cursor: SalatMongoCursor[Item], searchFields: SearchFields, current: Boolean = true): JsValue = {
    JsObject(Seq("count" -> JsNumber(count)))
  }
  def itemOnlyJson(count: Int, cursor: SalatMongoCursor[Item], searchFields: SearchFields, current: Boolean = true): JsValue = {
    val itemViews: Seq[ItemView] = cursor.toList.map(ItemView(_, Some(searchFields)))
    toJson(itemViews)
  }

  def parseCollectionIds[A](request: ApiRequest[A])(value: AnyRef): Either[error.InternalError, AnyRef] = value match {
    case dbo: BasicDBObject => dbo.toSeq.headOption match {
      case Some((key, dblist)) => if (key == "$in") {
        if (dblist.isInstanceOf[BasicDBList]) {
          try {
            if (dblist.asInstanceOf[BasicDBList].toArray.forall(coll => ContentCollection.isAuthorized(request.ctx.organization, new ObjectId(coll.toString), Permission.Read)))
              Right(value)
            else Left(InternalError("attempted to access a collection that you are not authorized to"))
          } catch {
            case e: IllegalArgumentException => Left(InternalError("could not parse collectionId into an object id", e))
          }
        } else Left(InternalError("invalid value for collectionId key. could not cast to array"))
      } else Left(InternalError("can only use $in special operator when querying on collectionId"))
      case None => Left(InternalError("empty db object as value of collectionId key"))
    }
    case _ => Left(InternalError("invalid value for collectionId"))
  }

  private def itemList[A](
                           q: Option[String],
                           f: Option[String],
                           sk: Int,
                           l: Int,
                           sort: Option[String],
                           collections: Seq[ObjectId],
                           current: Boolean = true,
                           jsBuilder: (Int, SalatMongoCursor[Item], SearchFields, Boolean) => JsValue)
                         (implicit request: ApiRequest[A]): Either[ApiError, JsValue] = {
    if (!collections.nonEmpty) {
      Right(JsArray(Seq()))
    } else {
      val initSearch: MongoDBObject = MongoDBObject(collectionId -> MongoDBObject("$in" -> collections.map(_.toString)))

      val queryResult: Either[SearchCancelled, MongoDBObject] = q.map(query => ItemSearch.toSearchObj(query,
        Some(initSearch),
        Map(collectionId -> parseCollectionIds(request))
      )) match {
        case Some(result) => result
        case None => Right(initSearch)
      }
      val fieldResult: Either[InternalError, SearchFields] = f.map(fields => ItemSearch.toFieldsObj(fields)) match {
        case Some(result) => result
        case None => Right(SearchFields(method = 1))
      }

      def runQueryAndMakeJson(query: MongoDBObject, fields: SearchFields, sk: Int, limit: Int, sortField: Option[MongoDBObject] = None) = {
        val cursor = service.find(query, fields.dbfields)
        val count = cursor.count
        val sorted = sortField.map(cursor.sort(_)).getOrElse(cursor)
        jsBuilder(count, sorted.skip(sk).limit(limit), fields, current)
      }

      queryResult match {
        case Right(query) => fieldResult match {
          case Right(searchFields) => {
            cleanDbFields(searchFields, request.ctx.isLoggedIn)
            sort.map(ItemSearch.toSortObj(_)) match {
              case Some(Right(sortField)) => Right(runQueryAndMakeJson(query, searchFields, sk, l, Some(sortField)))
              case None => Right(runQueryAndMakeJson(query, searchFields, sk, l))
              case Some(Left(error)) => Left(ApiError.InvalidFields(error.clientOutput))
            }
          }
          case Left(error) => Left(ApiError.InvalidFields(error.clientOutput))
        }
        case Left(sc) => sc.error match {
          case None => Right(JsArray(Seq()))
          case Some(error) => Left(ApiError.InvalidQuery(error.clientOutput))
        }
      }
    }
  }

  private def cleanDbFields(searchFields: SearchFields, isLoggedIn: Boolean, dbExtraFields: Seq[String] = dbSummaryFields, jsExtraFields: Seq[String] = jsonSummaryFields) {
    if (!isLoggedIn && searchFields.dbfields.isEmpty) {
      dbExtraFields.foreach(extraField =>
        searchFields.dbfields = searchFields.dbfields ++ MongoDBObject(extraField -> searchFields.method)
      )
      jsExtraFields.foreach(extraField =>
        searchFields.jsfields = searchFields.jsfields :+ extraField
      )
    }
    if (searchFields.method == 1 && searchFields.dbfields.nonEmpty) searchFields.dbfields = searchFields.dbfields
  }

  def listWithOrg(orgId: ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) = ApiAction {
    implicit request =>
      if (Organization.getTree(request.ctx.organization).exists(_.id == orgId)) {
        val collections = ContentCollection.getCollectionIds(orgId, Permission.Read)
        val jsonBuilder = if(c == "true") countOnlyJson _ else itemOnlyJson _
        itemList(q, f, sk, l, sort, collections, true, jsonBuilder) match {
          case Left(apiError) => BadRequest(toJson(apiError))
          case Right(json) => Ok(json)
        }
      } else Forbidden(toJson(ApiError.UnauthorizedOrganization))
  }

  def listWithColl(collId: ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) = ApiAction {
    implicit request =>
      if (ContentCollection.isAuthorized(request.ctx.organization, collId, Permission.Read)) {
        val jsBuilder = if(c == "true") countOnlyJson _ else itemOnlyJson _
        itemList(q, f, sk, l, sort, Seq(collId), true, jsBuilder) match {
          case Left(apiError) => BadRequest(toJson(apiError))
          case Right(json) => Ok(json)
        }
      } else Unauthorized(toJson(ApiError.UnauthorizedOrganization))
  }


  def update(id: VersionedId[ObjectId]) = ValidatedItemApiAction(id, Permission.Write) {
    request =>
      for {
        json <- request.body.asJson.toSuccess("No json in request body")
        item <- json.asOpt[Item].toSuccess("Bad json format - can't parse")
        dbitem <- service.findOneById(id).toSuccess("no item found for the given id")
        validatedItem <- validateItem(dbitem, item).toSuccess("Invalid data")
        savedResult <- saveItem(validatedItem, dbitem.published && (service.sessionCount(dbitem) > 0)).toSuccess("Error saving item")
      } yield savedResult
  }

  def cloneItem(id: VersionedId[ObjectId]) = ValidatedItemApiAction(id, Permission.Write) {
    request =>
      for {
        item <- service.findOneById(id).toSuccess("Can't find item")
        cloned <- service.cloneItem(item).toSuccess("Error cloning")
      } yield cloned
  }


  private def validateItem(dbItem:Item, item: Item): Option[Item] = {
    val itemCopy = item.copy(
      id = dbItem.id,
      collectionId = if (item.collectionId.isEmpty) dbItem.collectionId else item.collectionId
    )
    addStorageKeysToItem(dbItem,item)
    Some(itemCopy)
  }

  /** TODO: Remove code duplication here..
   * add storage keys to item before update
   * @param dbItem
   * @param item
   */
  private def addStorageKeysToItem(dbItem:Item, item:Item) = {
    val itemsf:Seq[StoredFile] =
      item.data.map(r => r.files.filter(_.isInstanceOf[StoredFile]).map(_.asInstanceOf[StoredFile])).getOrElse(Seq()) ++
        item.supportingMaterials.map(r => r.files.filter(_.isInstanceOf[StoredFile]).map(_.asInstanceOf[StoredFile])).flatten
    val dbitemsf:Seq[StoredFile] =
      dbItem.data.map(r => r.files.filter(_.isInstanceOf[StoredFile]).map(_.asInstanceOf[StoredFile])).getOrElse(Seq()) ++
        dbItem.supportingMaterials.map(r => r.files.filter(_.isInstanceOf[StoredFile]).map(_.asInstanceOf[StoredFile])).flatten
    itemsf.foreach(sf => {
      dbitemsf.find(_.name == sf.name) match {
        case Some(dbsf) => sf.storageKey = dbsf.storageKey
        case None => Logger.warn("addStorageKeysToItem: no db storage key found")
      }
    })
  }

  /** Note: we remove the version - so that the dao automatically returns the latest version
   */
  private def saveItem(item: Item, createNewVersion: Boolean): Option[Item] = {
    service.save(item, createNewVersion)
    val vid : VersionedId[ObjectId] = item.id.copy(version = None)
    service.findOneById(vid)
  }

  def get(id: VersionedId[ObjectId], detail: Option[String] = Some("normal")) = ItemApiAction(id, Permission.Read) {
    request =>

      val fields = detail.map{ d =>
        if(d == "detailed" ) getFieldsDbo(false, request.ctx.isLoggedIn, Seq(data)) else getFieldsDbo(true, request.ctx.isLoggedIn)
      }.getOrElse(getFieldsDbo(true, request.ctx.isLoggedIn))

      Logger.debug("[ItemApi.get] fields: " + fields)

      service.findFieldsById(id, fields)
        .map(dbo => com.novus.salat.grater[Item].asObject(dbo))
        .map(i => Ok(Json.toJson(i)))
        .getOrElse(NotFound)
  }

  def getDetail(id: VersionedId[ObjectId] ) = get(id, Some("detailed"))


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
          val orgName = Organization.findOneById(request.ctx.organization).map(_.name).getOrElse("unknown org")
          val message = "Access forbidden for org: " + orgName
          Forbidden(JsObject(Seq("message" -> JsString(message))))
        }
    }

  /** A wrapper around the dbfields logic
    * TODO: tidy up dbfields stuff
    */
  private def getFieldsDbo(include:Boolean, isLoggedIn : Boolean, otherFields : Seq[String] = Seq()) : MongoDBObject = {
    val searchFields = SearchFields(method = if(include) 1 else 0)
    cleanDbFields(searchFields, isLoggedIn, otherFields)
    searchFields.dbfields
  }

  /**
   * Deletes the item matching the id specified
   */
  def delete(id: VersionedId[ObjectId]) = ApiAction {
    request =>
      service.findFieldsById(id, MongoDBObject(collectionId -> 1)) match {
        case Some(o) => o.get(collectionId) match {
          case collId: String => if (Content.isCollectionAuthorized(request.ctx.organization, collId, Permission.Write)) {
            Content.moveToArchive(id) match {
              case Right(_) => Ok(com.mongodb.util.JSON.serialize(o))
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

  private def itemFromJson(json:JsValue) : Item = {
    json.asOpt[Item].getOrElse{
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
                    i.collectionId = default.id.toString
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


}

object ItemApi extends api.v1.ItemApi(CorespringS3ServiceImpl, ItemServiceImpl)
