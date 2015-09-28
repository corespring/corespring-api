package org.corespring.v2.player.hooks

import com.mongodb._
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.container.client.hooks.Hooks.StatusMessage
import org.corespring.models.Organization
import org.corespring.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.services.item.ItemService
import org.corespring.test.fakes.Fakes.withMockCollection
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.V2PlayerIntegrationSpec
import org.specs2.matcher.{ Expectable, Matcher }
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.RequestHeader

import scala.concurrent.Future
import scalaz.{ Failure, Success, Validation }

class ItemHooksTest extends V2PlayerIntegrationSpec {

  import scala.language.higherKinds

  implicit val ec = containerExecutionContext

  val emptyItemJson = Json.obj(
    "profile" -> Json.obj(),
    "components" -> Json.obj(),
    "xhtml" -> "<div/>",
    "summaryFeedback" -> "")

  val defaultFailure = generalError("Default failure")

  val defaultOrgAndOpts = mockOrgAndOpts()

  abstract class baseContext[ERR, RES](
    val itemId: String = ObjectId.get.toString,
    val authResult: Validation[V2Error, Item] = Failure(defaultFailure),
    val orgAndOptsResult: Validation[V2Error, OrgAndOpts] = Success(defaultOrgAndOpts)) extends Scope {

    lazy val itemTransformer = {
      val m = mock[ItemTransformer]
      m.transformToV2Json(any[Item]) returns Json.obj("transformed-item" -> true)
      m
    }

    lazy val itemAuth: ItemAuth[OrgAndOpts] = {
      val m = mock[ItemAuth[OrgAndOpts]]
      m.loadForRead(anyString)(any[OrgAndOpts]) returns authResult
      m.loadForWrite(anyString)(any[OrgAndOpts]) returns authResult
      m.canCreateInCollection(anyString)(any[OrgAndOpts]) returns authResult.map { i => true }
      m.insert(any[Item])(any[OrgAndOpts]) returns {
        if (ObjectId.isValid(itemId)) {
          Some(VersionedId(new ObjectId(itemId)))
        } else None
      }
      m
    }

    lazy val org = {
      val m = mock[Organization]
      m.id returns ObjectId.get
      m.name returns "mock org"
      m
    }

    def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = {
      orgAndOptsResult
    }

    val itemService = mock[ItemService]

    lazy val hooks = new ItemHooks(
      itemTransformer,
      itemAuth,
      itemService,
      getOrgAndOptions,
      containerExecutionContext)
  }

  class loadContext(
    itemId: String = ObjectId.get.toString,
    authResult: Validation[V2Error, Item] = Failure(defaultFailure))
    extends baseContext[StatusMessage, JsValue](itemId, authResult) {

    val result: Future[Either[StatusMessage, JsValue]] = hooks.load(itemId)
  }

  def returnError[D](e: V2Error) = returnStatusMessage[D](e.statusCode, e.message)

  case class returnStatusMessage[D](expectedStatus: Int, body: String) extends Matcher[Either[(Int, String), D]] {
    def apply[S <: Either[(Int, String), D]](s: Expectable[S]) = {

      def callResult(success: Boolean) = result(success, s"${s.value} matches $expectedStatus & $body", s"${s.value} doesn't match $expectedStatus & $body", s)
      s.value match {
        case Left((code, msg)) => callResult(code == expectedStatus && msg == body)
        case Right(_) => callResult(false)
      }
    }
  }

  class createContext(
    val json: Option[JsValue] = None,
    authResult: Validation[V2Error, Item] = Failure(defaultFailure))

    extends baseContext[(Int, String), String](authResult = authResult) {

    val result = hooks.createItem(json)
  }

  "load" should {

    "return can't find item id error" in new loadContext() {
      result must returnStatusMessage(defaultFailure.statusCode, defaultFailure.message).await
    }

    "return bad request for bad item id" in new loadContext("", authResult = Success(mockItem)) {
      result must returnError(cantParseItemId("")).await
    }

    "return org can't access item error" in new loadContext(authResult = Failure(generalError("NO!"))) {
      result must returnStatusMessage(authResult.toEither.left.get.statusCode, authResult.toEither.left.get.message).await
    }

    "return an item" in new loadContext(
      authResult = Success(Item(collectionId = ObjectId.get.toString))) {
      result must equalTo(Right(Json.obj("transformed-item" -> true))).await
      there was one(itemTransformer).transformToV2Json(any[Item])
    }
  }

  "create" should {

    "return no json error" in new createContext(None, Success(mockItem)) {
      result must returnError(noJson).await
    }

    "return property not found" in new createContext(Some(Json.obj()), Success(mockItem)) {
      result must returnError(propertyNotFoundInJson("collectionId")).await
    }

    "return no org id and options" in new createContext(
      Some(Json.obj("collectionId" -> ObjectId.get.toString))) {
      result must returnStatusMessage(defaultFailure.statusCode, defaultFailure.message).await
    }

    "return item id for new item" in new createContext(
      Some(Json.obj("collectionId" -> ObjectId.get.toString)),
      authResult = Success(mockItem)) {
      result.map(_.isRight) must equalTo(true).await
    }
  }

  "save***" should {

    val orgAndOptsForSpec = mockOrgAndOpts(AuthMode.AccessToken)

    //<<<<<<< HEAD
    class baseScope(override val orgAndOptsResult: Validation[V2Error, OrgAndOpts] = Success(orgAndOptsForSpec))
      extends baseContext with withMockCollection {

      def orgAndOptsErr = orgAndOptsResult.toEither.left.get

      itemAuth.canWrite(any[String])(any[OrgAndOpts]) returns Success(true)
      //=======
      //    class baseScope(orgAndOptsResult: Validation[V2Error, OrgAndOpts] = Success(orgAndOptsForSpec))
      //      extends Scope
      //      with ItemHooks with withMockCollection {
      //
      //      def orgAndOptsErr = orgAndOptsResult.toEither.left.get
      //
      //      lazy val vid = {
      //        val o = VersionedId(ObjectId.get, Some(0))
      //        o
      //      }
      //
      //      lazy val mockItemService = {
      //        val m = mock[ItemService]
      //        m.collection returns mockCollection
      //        m
      //      }
      //
      //      lazy val mockItemAuth = {
      //        val m = mock[ItemAuth[OrgAndOpts]]
      //        m.canWrite(any[String])(any[OrgAndOpts]) returns Success(true)
      //        m
      //      }
      //
      //      override def transform: (Item) => JsValue = i => Json.obj()
      //
      //      override def itemService: ItemService = mockItemService
      //
      //      override def auth: ItemAuth[OrgAndOpts] = mockItemAuth
      //
      //      override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = orgAndOptsResult
      //
      //      def waitFor[A](f: Future[A]) = Await.result(f, Duration(1, TimeUnit.SECONDS))
      //>>>>>>> develop

    }

    lazy val vid = VersionedId(ObjectId.get)

    class saveScope(
      fn: ItemHooks => String => Future[Either[(Int, String), JsValue]],
      val expectedSet: DBObject) extends baseScope {
      val expectedQuery = MongoDBObject("_id._id" -> vid.id)
      itemService.saveUsingDbo(any[VersionedId[ObjectId]], any[DBObject], any[Boolean]) returns true
      val r = waitFor(fn(hooks)(vid.toString))
      there was one(itemService).saveUsingDbo(vid, MongoDBObject("$set" -> expectedSet), false)
      //=======
      //      waitFor(fn(this)(vid.toString))
      //      val (q, u) = captureUpdate
      //      q.value === expectedQuery
      //      u.value === MongoDBObject("$set" -> expectedSet)
      //>>>>>>> develop
    }

    "save returns orgAndOpts error" in new baseScope(Failure(TestError("org-and-opts"))) {
      hooks.saveXhtml(vid.toString, "xhtml") must equalTo(
        Left(orgAndOptsErr.statusCode -> orgAndOptsErr.message)).await
    }

    "save returns cantParseItemId error" in new baseScope() {
      val err = cantParseItemId("bad id")
      hooks.saveXhtml("bad id", "xhtml") must equalTo(Left(err.statusCode -> err.message)).await
    }

    "save returns itemAuth.canWrite error" in new baseScope() {
      val err = TestError("can-write")
      itemAuth.canWrite(any[String])(any[OrgAndOpts]) returns Failure(err)
      hooks.saveXhtml(vid.toString, "xhtml") must equalTo(Left(err.statusCode -> err.message)).await
    }

    "save returns json" in new baseScope() {
      itemService.saveUsingDbo(any[VersionedId[ObjectId]], any[DBObject], any[Boolean]) returns true
      hooks.saveXhtml(vid.toString, "new-xhtml") must equalTo(Right(Json.obj("xhtml" -> "new-xhtml"))).await
    }

    "saveXhtml calls MongoCollection.update" in new saveScope(
      ih => ih.saveXhtml(_, "update"),
      MongoDBObject("playerDefinition.xhtml" -> "update")) {
    }

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
