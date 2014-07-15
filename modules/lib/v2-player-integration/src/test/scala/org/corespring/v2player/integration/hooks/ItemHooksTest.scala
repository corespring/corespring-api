package org.corespring.v2.player.hooks

import java.util.concurrent.TimeUnit

import org.bson.types.ObjectId
import org.corespring.container.client.hooks.Hooks.StatusMessage
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.matchers.RequestMatchers
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.specs2.matcher.{ Expectable, Matcher }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.http.Status._
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest

import scala.concurrent.{ Await, ExecutionContext, Future }
import scalaz.{ Failure, Success, Validation }

class ItemHooksTest extends Specification with Mockito with RequestMatchers {

  import scala.language.higherKinds

  val emptyItemJson = Json.obj(
    "profile" -> Json.obj(),
    "components" -> Json.obj(),
    "xhtml" -> "<div/>",
    "summaryFeedback" -> "")

  val defaultFailure = generalError("Default failure")

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

      override def auth: ItemAuth = {
        val m = mock[ItemAuth]
        m.loadForRead(anyString)(any[RequestHeader]) returns authResult
        m.loadForWrite(anyString)(any[RequestHeader]) returns authResult
        m.canCreateInCollection(anyString)(any[RequestHeader]) returns authResult.map { i => true }
        m.insert(any[Item])(any[RequestHeader]) returns Some(vid)
        m
      }

      override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global
    }
  }

  class loadContext(
    itemId: String = ObjectId.get.toString,
    authResult: Validation[V2Error, Item] = Failure(defaultFailure))
    extends baseContext[StatusMessage, JsValue](itemId, authResult) {

    val f: Future[Either[StatusMessage, JsValue]] = hooks.load(itemId)(FakeRequest("", ""))
  }

  class saveContext(itemId: String = ObjectId.get.toString,
    authResult: Validation[V2Error, Item] = Failure(defaultFailure),
    val json: JsValue = Json.obj()) extends baseContext[StatusMessage, JsValue](itemId, authResult) {

    lazy val f = hooks.save(itemId, json)(header)
  }

  def returnError[D](e: V2Error) = returnStatusMessage[D](e.statusCode, e.message)

  case class returnStatusMessage[D](expectedStatus: Int, body: String) extends Matcher[Either[(Int, String), D]] {
    def apply[S <: Either[(Int, String), D]](s: Expectable[S]) = {

      println(s" --> ${s.value}")
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

    override def f: Future[Either[(Int, String), String]] = hooks.create(json)(header)
  }

  "load" should {

    "return can't find item id error" in new loadContext() {
      result must returnStatusMessage(defaultFailure.statusCode, defaultFailure.message)
    }

    "return bad request for bad item id" in new loadContext("", authResult = Success(Item())) {
      result must returnError(cantParseItemId(""))
    }

    "return org can't access item error" in new loadContext(authResult = Failure(generalError("NO!"))) {
      result must returnStatusMessage(authResult.toEither.left.get.statusCode, authResult.toEither.left.get.message)
    }

    "return an item" in new loadContext(
      authResult = Success(Item(collectionId = Some(ObjectId.get.toString)))) {
      result must_== Right(Json.parse("""{"profile":{},"components":{},"xhtml":"<div/>","summaryFeedback":""}"""))
    }
  }

  "save" should {

    "return not found" in new saveContext() {
      result must returnStatusMessage(defaultFailure.statusCode, defaultFailure.message)
    }

    "return cantParse error for a bad item id" in new saveContext("bad id", authResult = Success(Item())) {
      result must returnError(cantParseItemId("bad id"))
    }

    "return no collection id error" in new saveContext(
      authResult = Success(Item())) {
      result must returnError(noCollectionIdForItem(vid))
    }

    "save update" in new saveContext(
      authResult = Success(Item(collectionId = Some(ObjectId.get.toString))),
      json = emptyItemJson) {
      result === Right(emptyItemJson)
    }
  }

  "create" should {

    "return no json error" in new createContext(None) {
      result must returnError(noJson)
    }

    "return property not found" in new createContext(Some(Json.obj())) {
      result must returnError(propertyNotFoundInJson("collectionId"))
    }

    "return no org id and options" in new createContext(
      Some(Json.obj("collectionId" -> ObjectId.get.toString))) {
      result must returnStatusMessage(defaultFailure.statusCode, defaultFailure.message)
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
}
