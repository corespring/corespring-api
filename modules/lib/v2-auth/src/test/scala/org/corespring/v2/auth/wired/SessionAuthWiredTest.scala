package org.corespring.v2.auth.wired

import org.bson.types.ObjectId
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.AuthMode.AuthMode
import org.corespring.v2.auth.models.AuthMode.AuthMode
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts, PlayerOptions }
import org.corespring.v2.errors.Errors.{ generalError, noItemIdInSession, cantLoadSession }
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest

import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

class SessionAuthWiredTest extends Specification with Mockito {

  val defaultItemFailure = generalError("no item")

  "SessionAuth" should {

    implicit val rh: OrgAndOpts = OrgAndOpts(ObjectId.get, PlayerOptions.ANYTHING, AuthMode.UserSession)

    case class authScope(
      session: Option[JsValue] = None,
      item: Option[Item] = None,
      itemLoadForRead: Boolean = true,
      itemLoadForWrite: Boolean = true,
      hasPerms: Validation[V2Error, Boolean] = Success(true),
      orgAndOpts: Validation[V2Error, (ObjectId, PlayerOptions, AuthMode)] = Success(ObjectId.get, PlayerOptions.ANYTHING, AuthMode.UserSession)) extends Scope {
      val auth = new SessionAuthWired {

        override def previewSessionService: MongoService = {
          val m = mock[MongoService]
          m.load(anyString) returns session.map(s => Json.obj("service" -> "preview") ++ s.as[JsObject])
          m
        }

        override def mainSessionService: MongoService = {
          val m = mock[MongoService]
          m.load(anyString) returns session.map(s => Json.obj("service" -> "main") ++ s.as[JsObject])
          m
        }

        override def itemAuth: ItemAuth[OrgAndOpts] = {
          val m = mock[ItemAuth[OrgAndOpts]]
          m.loadForRead(anyString)(any[OrgAndOpts]) returns (if (itemLoadForRead) item.toSuccess(defaultItemFailure) else Failure(defaultItemFailure))
          m.loadForWrite(anyString)(any[OrgAndOpts]) returns (if (itemLoadForWrite) item.toSuccess(defaultItemFailure) else Failure(defaultItemFailure))
          m
        }

        override def hasPermissions(itemId: String, sessionId: String, options: PlayerOptions): Validation[V2Error, Boolean] = {
          hasPerms
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

    def run(fn: (SessionAuthWired) => Validation[V2Error, (JsValue, Item)], serviceName: String) = {
      "fail if theres no session" in new authScope() {
        fn(auth) must_== Failure(cantLoadSession(""))
      }

      "fail if theres a session with no item id" in new authScope(session = Some(Json.obj())) {
        fn(auth) must_== Failure(noItemIdInSession(""))
      }

      "fail if there is no item" in new authScope(session = Some(Json.obj("itemId" -> "itemId"))) {
        fn(auth) must_== Failure(defaultItemFailure)
      }

      "succeed" in new authScope(
        item = Some(Item()),
        session = Some(Json.obj("itemId" -> "itemId"))) {
        val Success((rSession, rItem)) = fn(auth)
        (rSession.as[JsObject] - "service", rItem) must_== (session.get, item.get)
        (rSession \ "service").as[String] must_== serviceName
      }
    }

    def opts(m: AuthMode) = OrgAndOpts(ObjectId.get, PlayerOptions.ANYTHING, m)

    "load for write - user session - uses preview service" should {
      run(auth => auth.loadForWrite("")(opts(AuthMode.UserSession)), "preview")
    }

    "load for write - access token - uses main service" should {
      run(auth => auth.loadForWrite("")(opts(AuthMode.AccessToken)), "main")
    }

    "load for write - access token - uses main service" should {
      run(auth => auth.loadForWrite("")(opts(AuthMode.ClientIdAndOpts)), "main")
    }

    "load for read - user session - uses preview service" should {
      run(a => a.loadForRead("")(opts(AuthMode.UserSession)), "preview")
    }

    "load for read - access token - uses main service" should {
      run(a => a.loadForRead("")(opts(AuthMode.AccessToken)), "main")
    }

    "load for read - access token - uses main service" should {
      run(a => a.loadForRead("")(opts(AuthMode.ClientIdAndOpts)), "main")
    }

    "can load session for write if item is load for read only" in new authScope(
      item = Some(Item()),
      session = Some(Json.obj("itemId" -> "itemId")),
      itemLoadForWrite = false) {
      val Success((rSession, rItem)) = auth.loadForWrite("?")
      (rSession.as[JsObject] - "service", rItem) must_== (session.get, item.get)
    }

  }

}
