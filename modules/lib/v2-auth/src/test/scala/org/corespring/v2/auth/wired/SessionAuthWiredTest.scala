package org.corespring.v2.auth.wired

import org.bson.types.ObjectId
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.{ Item, PlayerDefinition }
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.AuthMode.AuthMode
import org.corespring.v2.auth.models._
import org.corespring.v2.errors.Errors.{ cantLoadSession, generalError, noItemIdInSession }
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsObject, JsValue, Json }

import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

class SessionAuthWiredTest extends Specification with Mockito with MockFactory {

  val defaultItemFailure = generalError("no item")

  "SessionAuth" should {

    implicit val rh: OrgAndOpts = OrgAndOpts(mockOrg, PlayerAccessSettings.ANYTHING, AuthMode.UserSession, None)

    case class authScope(
      session: Option[JsValue] = None,
      playerDefinition: Option[PlayerDefinition] = None,
      itemLoadForRead: Boolean = true,
      itemLoadForWrite: Boolean = true,
      hasPerms: Validation[V2Error, Boolean] = Success(true)) extends Scope {
      val auth = new SessionAuthWired {

        val previewSessionService: MongoService = {
          val m = mock[MongoService]
          m.load(anyString) returns session.map(s => Json.obj("service" -> "preview") ++ s.as[JsObject])
          m.save(anyString, any[JsValue]) returns {
            Some(Json.obj())
          }
          m
        }

        val mainSessionService: MongoService = {
          val m = mock[MongoService]
          m.load(anyString) returns session.map(s => Json.obj("service" -> "main") ++ s.as[JsObject])
          m.create(any[JsValue]) returns Some(ObjectId.get)
          m
        }

        override def itemAuth: ItemAuth[OrgAndOpts] = {
          val m = mock[ItemAuth[OrgAndOpts]]
          m.loadForRead(anyString)(any[OrgAndOpts]) returns (if (itemLoadForRead) playerDefinition.map(pd => Item(playerDefinition = Some(pd))).toSuccess(defaultItemFailure) else Failure(defaultItemFailure))
          m.loadForWrite(anyString)(any[OrgAndOpts]) returns (if (itemLoadForWrite) playerDefinition.map(pd => Item(playerDefinition = Some(pd))).toSuccess(defaultItemFailure) else Failure(defaultItemFailure))
          m
        }

        override def hasPermissions(itemId: String, sessionId: String, settings: PlayerAccessSettings): Validation[V2Error, Boolean] = {
          hasPerms
        }

        override def itemTransformer: ItemTransformer = {
          val m = mock[ItemTransformer]

          m.createPlayerDefinition(any[Item]) returns playerDefinition.get

          m
        }
      }
    }

    def getEmptyPlayerDefinition: PlayerDefinition = PlayerDefinition(
      Seq.empty,
      "",
      Json.obj(),
      "",
      None)
    "can create" should {
      "fail" in new authScope() {
        auth.canCreate("") must_== Failure(defaultItemFailure)
      }

      "succeed" in new authScope(playerDefinition = Some(getEmptyPlayerDefinition)) {
        auth.canCreate("") must_== Success(true)
      }
    }

    def run(fn: (SessionAuthWired) => Validation[V2Error, (JsValue, PlayerDefinition)], serviceName: String) = {
      "fail if theres no session" in new authScope() {
        fn(auth) must_== Failure(cantLoadSession(""))
      }

      "fail if theres a session with no item id" in new authScope(session = Some(Json.obj())) {
        fn(auth) must_== Failure(noItemIdInSession(""))
      }

      "fail if there is no item" in new authScope(session = Some(Json.obj("itemId" -> "itemId"))) {
        fn(auth) must_== Failure(defaultItemFailure)
      }

      "find the definition in the session" in new authScope(session = Some(
        Json.obj("item" -> Json.obj(
          "xhtml" -> "<h1>Hello World</h1>",
          "components" -> Json.obj())))) {

        val Success((rSession, rPlayerDef)) = fn(auth)
        (rSession \ "item").asOpt[JsObject] must_== None
        rPlayerDef.xhtml must_== "<h1>Hello World</h1>"
      }

      "succeed" in new authScope(
        playerDefinition = Some(getEmptyPlayerDefinition),
        session = Some(Json.obj("itemId" -> "itemId"))) {
        val Success((rSession, rItem)) = fn(auth)
        (rSession.as[JsObject] - "service", rItem) must_== (session.get, playerDefinition.get)
        (rSession \ "service").as[String] must_== serviceName
      }
    }

    def opts(m: AuthMode, clientId: Option[String] = None) = OrgAndOpts(mockOrg, PlayerAccessSettings.ANYTHING, m, clientId)

    "load for write - user session - uses preview service" should {
      run(auth => auth.loadForWrite("")(opts(AuthMode.UserSession)), "preview")
    }

    "load for write - access token - uses main service" should {
      run(auth => auth.loadForWrite("")(opts(AuthMode.AccessToken)), "main")
    }

    "load for write - access token - uses main service" should {
      run(auth => auth.loadForWrite("")(opts(AuthMode.ClientIdAndPlayerToken)), "main")
    }

    "load for read - user session - uses preview service" should {
      run(a => a.loadForRead("")(opts(AuthMode.UserSession)), "preview")
    }

    "load for read - access token - uses main service" should {
      run(a => a.loadForRead("")(opts(AuthMode.AccessToken)), "main")
    }

    "load for read - access token - uses main service" should {
      run(a => a.loadForRead("")(opts(AuthMode.ClientIdAndPlayerToken)), "main")
    }

    "when loading a session" should {
      "can load write if item is read only" in new authScope(
        playerDefinition = Some(getEmptyPlayerDefinition),
        session = Some(Json.obj("itemId" -> "itemId")),
        itemLoadForWrite = false) {
        val Success((rSession, rItem)) = auth.loadForWrite("?")
        (rSession.as[JsObject] - "service", rItem) must_== (session.get, playerDefinition.get)
      }
    }

    "when saving" should {
      "add the identity data to the session data" in new authScope() {
        val optsIn = opts(AuthMode.ClientIdAndPlayerToken, Some("1"))
        val saveFn = auth.saveSessionFunction(optsIn)
        saveFn.toOption.map(fn => fn("1", Json.obj()))
        there was one(auth.mainSessionService).save("1", Json.obj("identity" -> IdentityJson(optsIn)))
      }
    }

    "when creating" should {
      "add the identity data to the session data" in new authScope() {
        val optsIn = opts(AuthMode.ClientIdAndPlayerToken, Some("1"))
        auth.create(Json.obj())(optsIn)
        there was one(auth.mainSessionService).create(Json.obj("identity" -> IdentityJson(optsIn)))
      }
    }
  }

}
