package org.corespring.api.v1

import com.novus.salat.dao.SalatMongoCursor
import org.corespring.legacy.ServiceLookup
import org.corespring.models.item.{ Item, Alignments, TaskInfo, Content => CsContent }
import org.corespring.models.json.item.ContentView
import org.corespring.platform.core.controllers.auth.{ BaseApi }
import org.corespring.models.search.SearchFields
import org.corespring.services.item.BaseContentService
import play.api.libs.json.Json._
import play.api.libs.json._

/**
 * This is a superclass for any API Controller that manages Content. ContentApi should provide any functionality that
 * is common to routes associated for various Content subclasses. An implicit Writes for the Content subclass must be
 * provided so that the controller can serialize Content.
 */
abstract class ContentApi[ContentType <: CsContent[_]](service: BaseContentService[ContentType, _])(implicit writes: Writes[ContentView[ContentType]]) extends BaseApi {

  lazy val contentCollectionService = ServiceLookup.contentCollectionService

  import Item.Keys._

  /** Subclasses must define the contentType of the Content in the database **/
  def contentType: String

  val dbSummaryFields = Seq(collectionId, taskInfo, otherAlignments, standards, contributorDetails, published, "data", "playerDefinition")

  val jsonSummaryFields: Seq[String] = Seq("id",
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

  protected def countOnlyJson(count: Int, cursor: SalatMongoCursor[ContentType], searchFields: SearchFields,
    current: Boolean = true): JsValue = JsObject(Seq("count" -> JsNumber(count)))

  protected def countAndListJson(count: Int, cursor: SalatMongoCursor[ContentType], searchFields: SearchFields,
    current: Boolean = true): JsValue = {
    val contentViews: Seq[ContentView[ContentType]] = cursor.toList.map(ContentView(_))
    JsObject(Seq("count" -> JsNumber(count), "data" -> toJson(contentViews)))
  }

  protected def contentOnlyJson(count: Int, cursor: SalatMongoCursor[ContentType], searchFields: SearchFields,
    current: Boolean = true): JsValue = {
    val contentViews: Seq[ContentView[ContentType]] = cursor.toList.map(ContentView[ContentType](_))
    toJson(contentViews)
  }

  def cleanDbFields(searchFields: SearchFields, isLoggedIn: Boolean, dbExtraFields: Seq[String] = dbSummaryFields, jsExtraFields: Seq[String] = jsonSummaryFields) = {}

}

