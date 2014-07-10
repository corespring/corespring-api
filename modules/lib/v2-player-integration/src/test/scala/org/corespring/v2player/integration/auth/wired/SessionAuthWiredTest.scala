package org.corespring.v2player.integration.auth.wired

import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2player.integration.errors.Errors.{ noItemIdInSession, cantLoadSession }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ Json, JsValue }
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest

import scalaz.{ Validation, Success, Failure }
import scalaz.Scalaz._

class SessionAuthWiredTest extends Specification with Mockito {

  val defaultItemFailure = "no item"
  "SessionAuth" should {

    implicit val rh: RequestHeader = FakeRequest("", "")

    case class authScope(session: Option[JsValue] = None, item: Option[Item] = None) extends Scope {
      val auth = new SessionAuthWired {
        override def sessionService: MongoService = {
          val m = mock[MongoService]
          m.load(anyString) returns session
          m
        }

        override def itemAuth: ItemAuth = {
          val m = mock[ItemAuth]
          m.loadForRead(anyString)(any[RequestHeader]) returns item.toSuccess(defaultItemFailure)
          m.loadForWrite(anyString)(any[RequestHeader]) returns item.toSuccess(defaultItemFailure)
          m
        }
      }
    }

    "can create" should {
      "fail" in new authScope() {
        auth.canCreate("") must_== Failure(defaultItemFailure)
      }

      "succeed" in new authScope(item = Some(Item())) {
        auth.canCreate("") must_== Success(true)
      }
    }

    def run(fn: (SessionAuthWired) => Validation[String, (JsValue, Item)]) = {
      "fail if theres no session" in new authScope() {
        fn(auth) must_== Failure(cantLoadSession("").message)
      }

      "fail if theres a session with no item id" in new authScope(session = Some(Json.obj())) {
        fn(auth) must_== Failure(noItemIdInSession("").message)
      }

      "fail if there is no item" in new authScope(session = Some(Json.obj("itemId" -> "itemId"))) {
        fn(auth) must_== Failure(defaultItemFailure)
      }

      "succeed" in new authScope(
        item = Some(Item()),
        session = Some(Json.obj("itemId" -> "itemId"))) {
        fn(auth) must_== Success((session.get, item.get))
      }
    }

    "load for write" should {
      run(a => a.loadForWrite(""))
    }

    "load for read" should {
      run(a => a.loadForRead(""))
    }
  }

}
