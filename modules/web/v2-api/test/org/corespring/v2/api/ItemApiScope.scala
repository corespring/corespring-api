package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.itemSearch.ItemIndexService
import org.corespring.models.{ Standard, Subject }
import org.corespring.models.item.{ FieldValue, ComponentType, Item }
import org.corespring.models.json.JsonFormatting
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.OrganizationService
import org.corespring.services.item.ItemService
import org.corespring.v2.api.services.ScoreService
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.{ MockFactory, OrgAndOpts }
import org.corespring.v2.errors.V2Error
import org.specs2.matcher.{ Expectable, MatchResult, Matcher }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.http.HeaderNames
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ AnyContentAsJson, SimpleResult }
import play.api.test.{ PlaySpecification, FakeHeaders, FakeRequest }

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Success, Validation }

private[api] case class beCodeAndJson(code: Int, json: JsValue) extends Matcher[Future[SimpleResult]] {
  def apply(t: Expectable[Future[SimpleResult]]): MatchResult[Future[SimpleResult]] = {

    import scala.concurrent.duration._
    implicit val timeout = new akka.util.Timeout(1.second)
    val statusMatch = play.api.test.Helpers.status(t.value) == code
    val jsonMatch = play.api.test.Helpers.contentAsJson(t.value) == json

    (statusMatch, jsonMatch) match {
      case (false, false) => failure("json and statusCode don't match", t)
      case (true, false) => failure("json doesn't match", t)
      case (false, true) => failure("status code doesn't match", t)
      case (true, true) => success("json + statusCode are as expected", t)
    }
  }
}

private[api] trait ItemApiSpec extends PlaySpecification with Mockito with MockFactory {

  import ExecutionContext.Implicits.global

  def FakeJsonRequest(json: JsValue = Json.obj()): FakeRequest[AnyContentAsJson] = {
    FakeRequest(
      "",
      "",
      FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> Seq("application/json"))),
      AnyContentAsJson(json))
  }
}

private[api] trait ItemApiScope extends V2ApiScope with Scope with MockFactory {

  import ExecutionContext.Implicits.global

  val jsonFormatting = new JsonFormatting {
    override def findStandardByDotNotation: (String) => Option[Standard] = s => None

    override def countItemsInCollection(collectionId: ObjectId): Long = 0

    override def rootOrgId: ObjectId = ObjectId.get

    override def fieldValue: FieldValue = FieldValue()

    override def findSubjectById: (ObjectId) => Option[Subject] = id => None
  }

  protected def transformItemToJson(item: Item, detail: Option[String] = None): JsValue = {
    import jsonFormatting.{ item => f }
    Json.toJson(item)
  }

  lazy val collectionId = ObjectId.get

  def orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(mockOrgAndOpts())

  lazy val mockItemService: ItemService = mock[ItemService]

  lazy val mockScoreService: ScoreService = {
    val m = mock[ScoreService]
    m
  }

  lazy val mockItemAuth: ItemAuth[OrgAndOpts] = {
    val m = mock[ItemAuth[OrgAndOpts]]
    m
  }

  lazy val mockItemIndexService = {
    val m = mock[ItemIndexService]
    m.reindex(any[VersionedId[ObjectId]]) returns Future(Success(""))
    m
  }

  lazy val mockOrgService = {
    val m = mock[OrganizationService]
    m
  }

  var itemTypes = Seq.empty[ComponentType]

  lazy val mockJsonFormatting = {
    val m = mock[JsonFormatting]
    m
  }

  lazy val apiContext = ItemApiExecutionContext(ExecutionContext.Implicits.global)

  lazy val api = new ItemApi(
    mockItemService,
    mockOrgService,
    mockItemIndexService,
    mockItemAuth,
    itemTypes,
    mockScoreService,
    mockJsonFormatting,
    apiContext,
    getOrgAndOptionsFn)
}
