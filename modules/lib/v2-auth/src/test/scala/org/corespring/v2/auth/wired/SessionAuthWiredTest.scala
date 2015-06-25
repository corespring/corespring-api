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

    implicit val identity: OrgAndOpts = mockOrgAndOpts()

    case class authScope(
      session: Option[JsValue] = None,
      playerDefinition: Option[PlayerDefinition] = None,
      itemLoadForRead: Boolean = true,
      itemLoadForWrite: Boolean = true,
      hasPerms: Validation[V2Error, Boolean] = Success(true),
      savedId: ObjectId = new ObjectId()) extends Scope {
      val auth = new SessionAuthWired {

        private def serviceMock(key: String) = {
          val m = mock[MongoService]
          m.load(anyString) returns session.map(s => Json.obj("service" -> key) ++ s.as[JsObject])
          m.create(any[JsValue]) returns Some(savedId)
          m.save(anyString, any[JsValue]).answers { (args, value) =>
            {
              Some(args.asInstanceOf[Array[Object]](1).asInstanceOf[JsValue])
            }
          }
          m
        }

        val previewSessionService: MongoService = serviceMock("preview")
        val mainSessionService: MongoService = serviceMock("main")

        override def itemAuth: ItemAuth[OrgAndOpts] = {
          val m = mock[ItemAuth[OrgAndOpts]]
          m.loadForRead(anyString)(any[OrgAndOpts]) returns (if (itemLoadForRead) playerDefinition.map(pd => Item(playerDefinition = Some(pd))).toSuccess(defaultItemFailure) else Failure(defaultItemFailure))
          m.loadForWrite(anyString)(any[OrgAndOpts]) returns (if (itemLoadForWrite) playerDefinition.map(pd => Item(playerDefinition = Some(pd))).toSuccess(defaultItemFailure) else Failure(defaultItemFailure))
          m
        }

        override def hasPermissions(itemId: String, sessionId: Option[String], settings: PlayerAccessSettings): Validation[V2Error, Boolean] = {
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

    def opts(m: AuthMode, clientId: Option[String] = None) = OrgAndOpts(mockOrg(), PlayerAccessSettings.ANYTHING, m, clientId)

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

      "add the identity but not the apiClient id" in new authScope() {
        val optsIn = opts(AuthMode.AccessToken)
        val saveFn = auth.saveSessionFunction(optsIn)
        saveFn.toOption.map(fn => fn("1", Json.obj()))
        val identityJson = IdentityJson(optsIn)
        (identityJson \ "apiClientId").asOpt[String] must beNone
        there was one(auth.mainSessionService).save("1", Json.obj("identity" -> identityJson))
      }
    }

    "when creating" should {
      "add the identity data to the session data" in new authScope() {
        val optsIn = opts(AuthMode.ClientIdAndPlayerToken, Some("1"))
        auth.create(Json.obj())(optsIn)
        val captor = capture[JsValue]
        there was one(auth.mainSessionService).create(captor.capture)
        (captor.value \ "identity").as[JsObject] must_== IdentityJson(optsIn)
      }

      "add dateCreated to the session data" in new authScope() {
        val optsIn = opts(AuthMode.ClientIdAndPlayerToken, Some("1"))
        auth.create(Json.obj())(optsIn)
        val captor = capture[JsValue]
        there was one(auth.mainSessionService).create(captor.capture)
        (captor.value \ "dateCreated" \ "$date").asOpt[Long] must beSome[Long]
      }
    }

    "loadWithIdentity" should {
      val identity = Json.obj("this" -> "is", "my" -> "identity")

      "provides identity in response" in new authScope(session = Some(
        Json.obj("item" -> Json.obj(
          "xhtml" -> "<h1>Hello World</h1>"),
          "identity" -> identity,
          "components" -> Json.obj()))) {
        val optsIn = opts(AuthMode.ClientIdAndPlayerToken, Some("1"))
        auth.loadWithIdentity("")(optsIn) match {
          case Success((result, _)) => (result \ "identity") must be equalTo (identity)
          case _ => failure("nope nope")
        }
      }
    }

    "reopen" should {
      val optsIn = opts(AuthMode.ClientIdAndPlayerToken, Some("1"))

      "save into main service" in new authScope(session = Some(Json.obj())) {
        auth.reopen("")(optsIn)
        there was one(auth.mainSessionService).load(any[String])
      }

      "return session with 0 attempts" in new authScope(session = Some(Json.obj()), savedId = new ObjectId()) {
        auth.reopen("")(optsIn) match {
          case Success(json) => (json \ "attempts").as[Int] must be equalTo (0)
          case _ => Failure("reopen was unsuccessful")
        }
      }

    }

    "complete" should {
      val optsIn = opts(AuthMode.ClientIdAndPlayerToken, Some("1"))

      "save into main service" in new authScope(session = Some(Json.obj())) {
        auth.complete("")(optsIn)
        there was one(auth.mainSessionService).load(any[String])
      }

      "return session with isComplete true" in new authScope(session = Some(Json.obj()), savedId = new ObjectId()) {
        auth.complete("")(optsIn) match {
          case Success(json) => (json \ "isComplete").as[Boolean] must beTrue
          case _ => Failure("reopen was unsuccessful")
        }
      }

    }

    "cloneIntoPreview" should {
      val optsIn = opts(AuthMode.ClientIdAndPlayerToken, Some("1"))

      "load from main service" in new authScope(session = Some(Json.obj())) {
        auth.cloneIntoPreview("")(optsIn)
        there was one(auth.mainSessionService).load(any[String])
      }

      "save into preview service" in new authScope(session = Some(Json.obj())) {
        auth.cloneIntoPreview("")(optsIn)
        there was one(auth.previewSessionService).create(any[JsObject])
      }

      "return id of saved session" in new authScope(session = Some(Json.obj()), savedId = new ObjectId()) {
        auth.cloneIntoPreview("")(optsIn) must be equalTo (Success(savedId))
      }

    }

  }

}
