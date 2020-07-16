package org.corespring.api.v1

import com.mongodb.casbah.Imports._
import salat.Context
import salat.dao.SalatMongoCursor
import org.corespring.models.ContentCollection
import org.corespring.models.auth.Permission
import org.corespring.models.error.CorespringInternalError
import org.corespring.models.item.{ Content => CsContent, Item, Alignments, TaskInfo }
import org.corespring.platform.core.controllers.auth.{ ApiRequest, BaseApi }
import org.corespring.platform.core.models.search.ItemSearch
import org.corespring.platform.core.models.search.SearchCancelled
import org.corespring.platform.core.models.search.SearchFields
import org.corespring.services.{ OrgCollectionService, OrganizationService, ContentCollectionService }
import org.corespring.web.api.v1.errors.ApiError
import play.api.libs.json.Json._
import play.api.libs.json._
import play.api.mvc._

/**
 * This is a superclass for any API Controller that manages Content. ContentApi should provide any functionality that
 * is common to routes associated for various Content subclasses. An implicit Writes for the Content subclass must be
 * provided so that the controller can serialize Content.
 */
abstract class ContentApi[ContentType <: CsContent[_]](
  service: SalatContentService[ContentType, _],
  orgService: OrganizationService,
  orgCollectionService: OrgCollectionService,
  implicit val context: Context,
  implicit val writes: Writes[ContentView[ContentType]]) extends BaseApi {

  import org.corespring.models.item.Item.Keys._

  /** Subclasses must define the contentType of the Content in the database **/
  def contentType: String

  val dbSummaryFields = Seq(collectionId, taskInfo, otherAlignments, standards, contributorDetails, published, "data", "playerDefinition")

  val jsonSummaryFields: Seq[String] = Seq(
    "id",
    collectionId,
    TaskInfo.Keys.gradeLevel,
    TaskInfo.Keys.itemType,
    Alignments.Keys.keySkills,
    primarySubject,
    TaskInfo.Keys.domains,
    relatedSubject,
    standards,
    author,
    TaskInfo.Keys.title,
    "contentFormat",
    published)

  protected def getCollectionIds(orgId: ObjectId, p: Permission): Seq[ObjectId] = {
    orgCollectionService
      .getCollections(orgId, Permission.Read)
      .fold(_ => Seq.empty[ContentCollection], a => a)
      .map(_.id)
  }

  /**
   * An API action to list JSON representations of Content, using pagination parameters for skipping, offset, and
   * sorting.
   */
  def list(
    query: Option[String],
    fields: Option[String],
    count: String,
    skip: Int,
    limit: Int,
    sort: Option[String]) = ApiAction {
    implicit request =>
      val collections = getCollectionIds(request.ctx.orgId, Permission.Read)

      val jsonBuilder = if (count == "true") countOnlyJson _ else contentOnlyJson _
      contentList(query, fields, skip, limit, sort, collections, true, jsonBuilder) match {
        case Left(apiError) => BadRequest(toJson(apiError))
        case Right(json) => Ok(json)
      }
  }

  /**
   * An API action that lists paginated JSON representations Content, but also includes a count of overall number of
   * results in available. JSON matches the form:
   *
   * <pre>
   *  {
   *    count: X,
   *    data: { ... }
   *  }
   * </pre>
   */
  def listAndCount(query: Option[String], fields: Option[String], skip: Int, limit: Int,
    sort: Option[String]): Action[AnyContent] = ApiAction { implicit request =>
    val collections = getCollectionIds(request.ctx.orgId, Permission.Read)

    contentList(query, fields, skip, limit, sort, collections, true, countAndListJson) match {
      case Left(apiError) => BadRequest(toJson(apiError))
      case Right(json) => Ok(json)
    }
  }

  private def baseQuery = MongoDBObject("contentType" -> contentType)

  protected def countOnlyJson(count: Int, cursor: SalatMongoCursor[ContentType], searchFields: SearchFields,
    current: Boolean = true): JsValue = JsObject(Seq("count" -> JsNumber(count)))

  protected def countAndListJson(count: Int, cursor: SalatMongoCursor[ContentType], searchFields: SearchFields,
    current: Boolean = true): JsValue = {
    val contentViews: Seq[ContentView[ContentType]] = cursor.toList.map(ContentView(_, Some(searchFields)))
    JsObject(Seq("count" -> JsNumber(count), "data" -> toJson(contentViews)))
  }

  protected def contentOnlyJson(count: Int, cursor: SalatMongoCursor[ContentType], searchFields: SearchFields,
    current: Boolean = true): JsValue = {
    val contentViews: Seq[ContentView[ContentType]] = cursor.toList.map(ContentView[ContentType](_, Some(searchFields)))
    toJson(contentViews)
  }

  private def createDefaultCollectionsQuery[A](collections: Seq[ObjectId], orgId: ObjectId): DBObject = {
    // filter the collections to exclude any that are not currently enabled for the organization
    val org = orgService.findOneById(orgId)
    val disabledCollections: Seq[ObjectId] = org match {
      case Some(organization) => organization.contentcolls.filterNot(collRef => collRef.enabled).map(_.collectionId)
      case None => Seq()
    }
    val enabledCollections = collections.filterNot(disabledCollections.contains(_))
    val collectionIdQry: MongoDBObject = MongoDBObject(collectionId -> MongoDBObject("$in" -> enabledCollections.map(_.toString)))
    val sharedInCollectionsQry: MongoDBObject = MongoDBObject(sharedInCollections -> MongoDBObject("$in" -> enabledCollections))
    val initSearch: MongoDBObject = MongoDBObject("$or" -> MongoDBList(collectionIdQry, sharedInCollectionsQry))
    initSearch
  }

  private def parseCollectionIds[A](organizationId: ObjectId)(value: AnyRef): Either[CorespringInternalError, AnyRef] = value match {
    case dbo: BasicDBObject => dbo.toSeq.headOption match {
      case Some((key, dblist)) => if (key == "$in") {
        if (dblist.isInstanceOf[BasicDBList]) {
          try {
            if (dblist.asInstanceOf[BasicDBList].toArray.forall(coll => orgCollectionService.isAuthorized(organizationId, new ObjectId(coll.toString), Permission.Read)))
              Right(value)
            else Left(CorespringInternalError("attempted to access a collection that you are not authorized to"))
          } catch {
            case e: IllegalArgumentException => Left(CorespringInternalError("could not parse collectionId into an object id", e))
          }
        } else Left(CorespringInternalError("invalid value for collectionId key. could not cast to array"))
      } else Left(CorespringInternalError("can only use $in special operator when querying on collectionId"))
      case None => Left(CorespringInternalError("empty db object as value of collectionId key"))
    }
    case _ => Left(CorespringInternalError("invalid value for collectionId"))
  }

  protected def contentList[A](
    q: Option[String],
    f: Option[String],
    sk: Int,
    l: Int,
    sort: Option[String],
    collections: Seq[ObjectId],
    current: Boolean = true,
    jsBuilder: (Int, SalatMongoCursor[ContentType], SearchFields, Boolean) => JsValue)(implicit request: ApiRequest[A]): Either[ApiError, JsValue] = {
    if (collections.isEmpty) {
      Right(JsArray(Seq()))
    } else {
      val initSearch: MongoDBObject = baseQuery.iterator.toSeq
        .foldLeft(createDefaultCollectionsQuery(collections, request.ctx.orgId)) { (query, entry) =>
          query ++ entry
        }

      val queryResult: Either[SearchCancelled, DBObject] = q.map(query => ItemSearch.toSearchObj(
        query,
        Some(initSearch),
        Map(collectionId -> parseCollectionIds(request.ctx.orgId)))) match {
        case Some(result) => result
        case None => Right(initSearch)
      }

      val fieldResult: Either[CorespringInternalError, SearchFields] = f.map(fields => ItemSearch.toFieldsObj(fields)) match {
        case Some(result) => result
        case None => Right(SearchFields(method = 1))
      }

      logger.trace(s"fieldResult: $fieldResult")

      def runQueryAndMakeJson(query: MongoDBObject, fields: SearchFields, sk: Int, limit: Int, sortField: Option[MongoDBObject] = None) = {
        logger.debug(s"query=${com.mongodb.util.JSON.serialize(query)}")
        val fieldsWithCollectionId = MongoDBObject(collectionId -> 1) ++ fields.fieldsToReturn
        logger.debug(s"fields=${fieldsWithCollectionId}")
        val cursor = service.find(query, fieldsWithCollectionId)
        val count = cursor.count
        val sorted = sortField.map(cursor.sort(_)).getOrElse(cursor)
        jsBuilder(count, sorted.skip(sk).limit(limit), fields, current)
      }

      queryResult match {
        case Right(query) => fieldResult match {
          case Right(searchFields) => {
            cleanDbFields(searchFields, request.ctx.isLoggedInUser)
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

  def cleanDbFields(searchFields: SearchFields, isLoggedIn: Boolean, dbExtraFields: Seq[String] = dbSummaryFields, jsExtraFields: Seq[String] = jsonSummaryFields) = {}

}

