package org.corespring.v2.player.hooks

import java.util.concurrent.TimeUnit

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{ MongoCollection, Imports }
import com.mongodb._
import org.bson.types.ObjectId
import org.corespring.container.client.hooks.Hooks.StatusMessage
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.PlaySingleton
import org.corespring.test.fakes.Fakes
import org.corespring.test.fakes.Fakes.withMockCollection
import org.corespring.test.matchers.RequestMatchers
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.{ MockFactory, AuthMode, PlayerAccessSettings, OrgAndOpts }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.integration.hooks.beErrorCodeMessage
import org.specs2.matcher.{ Expectable, Matcher }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.http.Status._
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }
import scalaz.{ Failure, Success, Validation }

class ItemHooksTest extends Specification with Mockito with RequestMatchers with MockFactory {

  PlaySingleton.start()

  import scala.language.higherKinds

  val emptyItemJson = Json.obj(
    "profile" -> Json.obj(),
    "components" -> Json.obj(),
    "xhtml" -> "<div/>",
    "summaryFeedback" -> "")

  val defaultFailure = generalError("Default failure")

  def TestError(msg: String) = generalError(msg)

  abstract class baseContext[ERR, RES](val itemId: String = ObjectId.get.toString,
    val authResult: Validation[V2Error, Item] = Failure(defaultFailure)) extends Scope {

    lazy val vid = VersionedId(new ObjectId(itemId))

    implicit lazy val header = FakeRequest("", "")

    def f: Future[Either[ERR, RES]]

    def result: Either[ERR, RES] = {
      import scala.concurrent.duration._
      Await.result(f, Duration(10, TimeUnit.SECONDS))
    }

    lazy val hooks = new ItemHooks {

      override def transform: (Item) => JsValue = (i: Item) => emptyItemJson

      override def auth: ItemAuth[OrgAndOpts] = {
        val m = mock[ItemAuth[OrgAndOpts]]
        m.loadForRead(anyString)(any[OrgAndOpts]) returns authResult
        m.loadForWrite(anyString)(any[OrgAndOpts]) returns authResult
        m.canCreateInCollection(anyString)(any[OrgAndOpts]) returns authResult.map { i => true }
        m.insert(any[Item])(any[OrgAndOpts]) returns Some(vid)
        m
      }

      override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

      lazy val org = {
        val m = mock[Organization]
        m.id returns ObjectId.get
        m.name returns "mock org"
        m
      }

      override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = authResult.map(_ => OrgAndOpts(org, PlayerAccessSettings.ANYTHING, AuthMode.AccessToken, None))

      def mockItemService = mock[ItemService]

      override def itemService: ItemService = mockItemService
    }
  }

  class loadContext(
    itemId: String = ObjectId.get.toString,
    authResult: Validation[V2Error, Item] = Failure(defaultFailure))
    extends baseContext[StatusMessage, JsValue](itemId, authResult) {

    val f: Future[Either[StatusMessage, JsValue]] = hooks.load(itemId)(FakeRequest("", ""))
  }

  def returnError[D](e: V2Error) = beErrorCodeMessage[D](e.statusCode, e.message)

  class createContext(
    val json: Option[JsValue] = None,
    authResult: Validation[V2Error, Item] = Failure(defaultFailure))

    extends baseContext[(Int, String), String](authResult = authResult) {

    override def f: Future[Either[(Int, String), String]] = hooks.createItem(json)(header)
  }

  "load" should {

    "return can't find item id error" in new loadContext() {
      result must beErrorCodeMessage(defaultFailure.statusCode, defaultFailure.message)
    }

    "return bad request for bad item id" in new loadContext("", authResult = Success(Item())) {
      result must returnError(cantParseItemId(""))
    }

    "return org can't access item error" in new loadContext(authResult = Failure(generalError("NO!"))) {
      result must beErrorCodeMessage(authResult.toEither.left.get.statusCode, authResult.toEither.left.get.message)
    }

    "return an item" in new loadContext(
      authResult = Success(Item(collectionId = Some(ObjectId.get.toString)))) {
      result must_== Right(Json.parse("""{"profile":{},"components":{},"xhtml":"<div/>","summaryFeedback":""}"""))
    }
  }

  "create" should {

    "return no json error" in new createContext(None, Success(Item())) {
      result must returnError(noJson)
    }

    "return property not found" in new createContext(Some(Json.obj()), Success(Item())) {
      result must returnError(propertyNotFoundInJson("collectionId"))
    }

    "return no org id and options" in new createContext(
      Some(Json.obj("collectionId" -> ObjectId.get.toString))) {
      result must beErrorCodeMessage(defaultFailure.statusCode, defaultFailure.message)
    }

    "return item id for new item" in new createContext(
      Some(Json.obj("collectionId" -> ObjectId.get.toString)),
      authResult = Success(Item())) {
      result match {
        case Left(e) => failure
        case Right(s) => success
      }
    }
  }

  "save***" should {

    implicit val r = FakeRequest("", "")

    val orgAndOptsForSpec = mockOrgAndOpts(AuthMode.AccessToken)

    class baseScope(orgAndOptsResult: Validation[V2Error, OrgAndOpts] = Success(orgAndOptsForSpec))
      extends Scope
      with ItemHooks with withMockCollection {

      def orgAndOptsErr = orgAndOptsResult.toEither.left.get

      lazy val vid = {
        val o = VersionedId(ObjectId.get, Some(0))
        o
      }

      lazy val mockItemService = {
        val m = mock[ItemService]
        m.collection returns mockCollection
        m
      }

      lazy val mockItemAuth = {
        val m = mock[ItemAuth[OrgAndOpts]]
        m.canWrite(any[String])(any[OrgAndOpts]) returns Success(true)
        m
      }

      override def transform: (Item) => JsValue = i => Json.obj()

      override def itemService: ItemService = mockItemService

      override def auth: ItemAuth[OrgAndOpts] = mockItemAuth

      override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = orgAndOptsResult

      def waitFor[A](f: Future[A]) = Await.result(f, Duration(1, TimeUnit.SECONDS))

    }

    class saveScope(
      fn: ItemHooks => String => Future[Either[(Int, String), JsValue]],
      expectedSet: DBObject) extends baseScope {

      val expectedQuery = MongoDBObject("_id._id" -> vid.id)
      waitFor(fn(this)(vid.toString))
      val (q, u) = captureUpdate
      q.value === expectedQuery
      u.value === MongoDBObject("$set" -> expectedSet)
    }

    "save returns orgAndOpts error" in new baseScope(Failure(TestError("org-and-opts"))) {
      waitFor(saveXhtml(vid.toString, "xhtml")) === Left(orgAndOptsErr.statusCode -> orgAndOptsErr.message)
    }

    "save returns cantParseItemId error" in new baseScope() {
      val err = cantParseItemId("bad id")
      waitFor(saveXhtml("bad id", "xhtml")) === Left(err.statusCode -> err.message)
    }

    "save returns itemAuth.canWrite error" in new baseScope() {
      val err = TestError("can-write")
      mockItemAuth.canWrite(any[String])(any[OrgAndOpts]) returns Failure(err)
      waitFor(saveXhtml(vid.toString, "xhtml")) === Left(err.statusCode -> err.message)
    }

    "save returns json" in new baseScope() {
      waitFor(saveXhtml(vid.toString, "new-xhtml")) === Right(Json.obj("xhtml" -> "new-xhtml"))
    }

    "saveXhtml calls MongoCollection.update" in new saveScope(
      ih => ih.saveXhtml(_, "update"),
      MongoDBObject("playerDefinition.xhtml" -> "update"))

    "saveCollectionId calls MongoCollection.update" in new saveScope(
      h => h.saveCollectionId(_, "new-id"),
      MongoDBObject("collectionId" -> "new-id"))

    "saveCustomScoring calls MongoCollection.update" in new saveScope(
      h => h.saveCustomScoring(_, "customScoring"),
      MongoDBObject("playerDefinition.customScoring" -> "customScoring"))

    "saveSupportingMaterials calls MongoCollection.update" in new saveScope(
      h => h.saveSupportingMaterials(_, Json.obj("new" -> true)),
      MongoDBObject("playerDefinition.supportingMaterials" -> MongoDBObject("new" -> true)))

    "saveComponents calls MongoCollection.update" in new saveScope(
      h => h.saveComponents(_, Json.obj("new" -> true)),
      MongoDBObject("playerDefinition.components" -> MongoDBObject("new" -> true)))

    "saveSummaryFeedback calls MongoCollection.update" in new saveScope(
      h => h.saveSummaryFeedback(_, "summary-feedback"),
      MongoDBObject("playerDefinition.summaryFeedback" -> "summary-feedback"))

    "saveProfile calls MongoCollection.update" in new saveScope(
      h => h.saveProfile(_, Json.obj("profile" -> "new")),
      MongoDBObject("playerDefinition.profile" -> MongoDBObject("profile" -> "new")))

  }
}
