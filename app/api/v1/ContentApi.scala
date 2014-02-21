package api.v1

import org.corespring.platform.core.models.item._
import controllers.auth.BaseApi
import play.api.mvc._
import com.mongodb.casbah.Imports._
import org.corespring.platform.core.models.search.ItemSearch
import play.api.libs.json._
import api.ApiError
import org.corespring.platform.core.models.item.Item.Keys._
import org.corespring.platform.core.models.error.InternalError
import org.corespring.platform.core.services.BaseContentService
import play.api.libs.json.Json._
import org.corespring.platform.core.models.search.SearchCancelled
import play.api.libs.json.JsArray
import scala.Some
import play.api.libs.json.JsNumber
import com.novus.salat.dao.SalatMongoCursor
import org.corespring.platform.core.models.search.SearchFields
import controllers.auth.ApiRequest
import org.corespring.platform.core.models.item.json.ContentView
import play.api.libs.json.JsObject
import org.corespring.platform.core.models.item.Content
import org.corespring.platform.core.models.ContentCollection
import org.corespring.platform.core.models.auth.Permission

abstract class ContentApi[ContentType <: Content[_]](service: BaseContentService[ContentType, _])
                                                    (implicit writes: Writes[ContentView[ContentType]]) extends BaseApi {

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

  def contentType: String
  private def baseQuery = MongoDBObject("contentType" -> contentType)

  protected def countOnlyJson(count: Int, cursor: SalatMongoCursor[ContentType], searchFields: SearchFields,
                              current: Boolean = true): JsValue = JsObject(Seq("count" -> JsNumber(count)))


  protected def countAndListJson(count: Int, cursor: SalatMongoCursor[ContentType], searchFields: SearchFields,
                                 current: Boolean = true): JsValue = {
    val contentViews: Seq[ContentView[ContentType]] = cursor.toList.map(ContentView(_, Some(searchFields)))
    JsObject(Seq("count" -> JsNumber(count), "data" -> toJson(contentViews)))
  }

  protected def contentOnlyJson(count: Int, cursor: SalatMongoCursor[ContentType], searchFields: SearchFields, current: Boolean = true): JsValue = {
    val contentViews: Seq[ContentView[ContentType]] = cursor.toList.map(ContentView[ContentType](_, Some(searchFields)))
    toJson(contentViews)
  }

  def list(query: Option[String],
           fields: Option[String],
           count: String,
           skip: Int,
           limit: Int,
           sort: Option[String]) = ApiAction {
    implicit request =>
      val collections = ContentCollection.getCollectionIds(request.ctx.organization, Permission.Read)

      val jsonBuilder = if (count == "true") countOnlyJson _ else contentOnlyJson _
      contentList(query, fields, skip, limit, sort, collections, true, jsonBuilder) match {
        case Left(apiError) => BadRequest(toJson(apiError))
        case Right(json) => Ok(json)
      }
  }

  def listAndCount(query: Option[String], fields: Option[String], skip: Int, limit: Int,
                   sort: Option[String]): Action[AnyContent] = ApiAction { implicit request =>
    val collections = ContentCollection.getCollectionIds(request.ctx.organization, Permission.Read)

    contentList(query, fields, skip, limit, sort, collections, true, countAndListJson) match {
      case Left(apiError) => BadRequest(toJson(apiError))
      case Right(json) => Ok(json)
    }
  }

  protected def contentList[A](q: Option[String],
                               f: Option[String],
                               sk: Int,
                               l: Int,
                               sort: Option[String],
                               collections: Seq[ObjectId],
                               current: Boolean = true,
                               jsBuilder: (Int, SalatMongoCursor[ContentType], SearchFields, Boolean) => JsValue)
                              (implicit request: ApiRequest[A]): Either[ApiError, JsValue] = {
    if (collections.isEmpty) {
      Right(JsArray(Seq()))
    } else {
      val initSearch: MongoDBObject = baseQuery.iterator.toSeq
        .foldLeft(service.createDefaultCollectionsQuery(collections, request.ctx.organization)){ (query, entry) =>
          query ++ entry }

      val queryResult: Either[SearchCancelled, MongoDBObject] = q.map(query => ItemSearch.toSearchObj(query,
        Some(initSearch),
        Map(collectionId -> service.parseCollectionIds(request.ctx.organization)))) match {
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

  protected def cleanDbFields(searchFields: SearchFields, isLoggedIn: Boolean, dbExtraFields: Seq[String] = dbSummaryFields, jsExtraFields: Seq[String] = jsonSummaryFields) {
    if (!isLoggedIn && searchFields.dbfields.isEmpty) {
      dbExtraFields.foreach(extraField =>
        searchFields.dbfields = searchFields.dbfields ++ MongoDBObject(extraField -> searchFields.method))
      jsExtraFields.foreach(extraField =>
        searchFields.jsfields = searchFields.jsfields :+ extraField)
    }
    if (searchFields.method == 1 && searchFields.dbfields.nonEmpty) searchFields.dbfields = searchFields.dbfields
  }

}
