package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.itemSearch.ItemIndexService
import org.corespring.models.item.{ ComponentType, FieldValue, Item }
import org.corespring.models.json.JsonFormatting
import org.corespring.models.{ Standard, Subject }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.{CloneItemService, OrgCollectionService, OrganizationService}
import org.corespring.services.item.ItemService
import org.corespring.v2.api.services.ScoreService
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.{ MockFactory, OrgAndOpts }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.sessiondb.SessionService
import org.specs2.matcher.{ ThrownExpectations, Expectable, MatchResult, Matcher }
import org.specs2.specification.Scope
import play.api.http.HeaderNames
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ AnyContentAsJson, SimpleResult }
import play.api.test.{ FakeHeaders, FakeRequest }

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Success, Validation }

private[api] case class beCodeAndJson(code: Int, json: JsValue) extends Matcher[Future[SimpleResult]] {

  def apply[S <: Future[SimpleResult]](t: Expectable[S]): MatchResult[S] = {

    import play.api.test.Helpers.{ status, contentAsJson }

    import scala.concurrent.duration._
    implicit val timeout = new akka.util.Timeout(10.second)
    val statusMatch = status(t.value) == code
    val jsonMatch = contentAsJson(t.value) == json

    def jsonFailed = s"json doesn't match. expected: $json, actual: ${contentAsJson(t.value)}."
    def statusFailed = s"status doesn't match. expected: $code, actual: ${status(t.value)}."
    (statusMatch, jsonMatch) match {
      case (false, false) => failure(s"$jsonFailed $statusFailed", t)
      case (true, false) => failure(jsonFailed, t)
      case (false, true) => failure(statusFailed, t)
      case (true, true) => success("json + statusCode are as expected", t)
    }
  }
}

private[api] trait ItemApiSpec extends V2ApiSpec {

  lazy val itemId = VersionedId(ObjectId.get)

  def FakeJsonRequest(json: JsValue = Json.obj()): FakeRequest[AnyContentAsJson] = {
    FakeRequest(
      "",
      "",
      FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> Seq("application/json"))),
      AnyContentAsJson(json))
  }
}

private[api] trait ItemApiScope extends V2ApiScope with Scope with MockFactory with ThrownExpectations {

  import ExecutionContext.Implicits.global

  val jsonFormatting = new JsonFormatting {
    override def findStandardByDotNotation: (String) => Option[Standard] = s => None

    override def rootOrgId: ObjectId = ObjectId.get

    override def fieldValue: FieldValue = new FieldValue()

    override def findSubjectById: (ObjectId) => Option[Subject] = id => None
  }

  protected def transformItemToJson(item: Item, detail: Option[String] = None): JsValue = {
    import jsonFormatting.{ item => f }
    Json.toJson(item)
  }

  lazy val collectionId = ObjectId.get

  def orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(mockOrgAndOpts())

  lazy val itemService: ItemService = mock[ItemService]

  lazy val scoreService: ScoreService = {
    val m = mock[ScoreService]
    m
  }

  lazy val itemAuth: ItemAuth[OrgAndOpts] = {
    val m = mock[ItemAuth[OrgAndOpts]]
    m
  }

  lazy val itemIndexService = {
    val m = mock[ItemIndexService]
    m.reindex(any[VersionedId[ObjectId]]) returns Future(Success(""))
    m
  }

  lazy val orgService = {
    val m = mock[OrganizationService]
    m
  }

  lazy val orgCollectionService = {
    val m = mock[OrgCollectionService]
    m
  }

  lazy val sessionService = {
    val m = mock[SessionService]
    m
  }

  lazy val cloneItemService = {
    val m = mock[CloneItemService]
    m
  }

  var itemTypes = Seq.empty[ComponentType]

  lazy val apiContext = ItemApiExecutionContext(ExecutionContext.Implicits.global)

  lazy val api = new ItemApi(
    itemService,
    orgService,
    orgCollectionService,
    cloneItemService,
    itemIndexService,
    itemAuth,
    itemTypes,
    scoreService,
    jsonFormatting,
    apiContext,
    sessionService,
    getOrgAndOptionsFn)
}
