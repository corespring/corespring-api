package org.corespring.v2.player.hooks

import org.bson.types.ObjectId
import org.corespring.container.client.hooks.Hooks.StatusMessage
import org.corespring.conversion.qti.transformers.{ ItemTransformer, PlayerJsonToItem }
import org.corespring.models.{ ContentCollection, Organization }
import org.corespring.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.OrgCollectionService
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.V2PlayerIntegrationSpec
import org.specs2.matcher.{ Expectable, Matcher }
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.RequestHeader

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

import scalaz.{ Failure, Success, Validation }

class ItemHooksTest extends V2PlayerIntegrationSpec with NoTimeConversions {

  import scala.language.higherKinds

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
    val orgAndOptsResult: Validation[V2Error, OrgAndOpts] = Success(defaultOrgAndOpts))
    extends Scope with StubJsonFormatting {

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

    val playerJsonToItem = new PlayerJsonToItem(jsonFormatting)

    lazy val orgCollectionService = {
      val m = mock[OrgCollectionService]
      m.getDefaultCollection(any[ObjectId]) returns Success(ContentCollection("coll", ObjectId.get))
      m
    }

    lazy val hooks = new ItemHooks(
      itemTransformer,
      itemAuth,
      itemService,
      jsonFormatting,
      playerJsonToItem,
      getOrgAndOptions,
      orgCollectionService,
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
    val collectionId: Option[String] = None,
    authResult: Validation[V2Error, Item] = Failure(defaultFailure))

    extends baseContext[(Int, String), String](authResult = authResult) {

    val result = hooks.createItem(collectionId)
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

    "return no org id and options" in new createContext(
      Some(ObjectId.get.toString)) {
      result must returnStatusMessage(defaultFailure.statusCode, defaultFailure.message).await
    }

    "return item id for new item" in new createContext(
      Some(ObjectId.get.toString),
      authResult = Success(mockItem)) {
      result.map(_.isRight) must equalTo(true).await
    }
  }

  "createSingleComponentItem" should {

    trait createSingleComponentItem extends baseContext[(Int, String), String] {
      override val authResult = Success(mockItem)
      lazy val captor = capture[Item]
      val result = Await.result(hooks.createSingleComponentItem(None, "component", "key", Json.obj("a" -> "b")), 1.second)
      there was one(itemAuth).insert(captor)(any[OrgAndOpts])
    }

    "call auth.insert with singleComponent xhtml" in new createSingleComponentItem {
      captor.value.playerDefinition.map(_.xhtml) must_== Some("""<div><div component="" id="key"></div></div>""")
    }

    "call auth.insert with singleComponent json" in new createSingleComponentItem {
      captor.value.playerDefinition.map(_.components) must_== Some(Json.obj("key" -> Json.obj("a" -> "b")))
    }

    "return the id" in new createSingleComponentItem {
      result.right.get must_== itemId.toString
    }
  }

}
