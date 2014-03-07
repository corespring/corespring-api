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
import org.corespring.search.ItemSearch
import org.corespring.platform.core.models.search.ItemSearch
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * This is a superclass for any API Controller that manages Content. ContentApi should provide any functionality that
 * is common to routes associated for various Content subclasses. An implicit Writes for the Content subclass must be
 * provided so that the controller can serialize Content.
 */
abstract class ContentApi[ContentType <: Content[_]](service: BaseContentService[ContentType, _], itemSearch: Option[ItemSearch])
                                                    (implicit writes: Writes[ContentView[ContentType]]) extends BaseApi {


  /** Subclasses must define the contentType of the Content in the database **/
  def contentType: String

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
   * An API action to list JSON representations of Content, using pagination parameters for skipping, offset, and
   * sorting.
   */
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
    val collections = ContentCollection.getCollectionIds(request.ctx.organization, Permission.Read)

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
      itemSearch match {
        case Some(search) => {
          Right(Await.result(search.find(
            queryString = q,
            collectionIds = collections,
            fields = Seq.empty,
            skip = Option(sk),
            limit = Option(l),
            sort = sort), Duration.Inf))
        }
        case _ => Left(ApiError.BadJson)
      }
    }
  }

  def cleanDbFields(searchFields: SearchFields, isLoggedIn: Boolean, dbExtraFields: Seq[String] = dbSummaryFields, jsExtraFields: Seq[String] = jsonSummaryFields) = {}

}
