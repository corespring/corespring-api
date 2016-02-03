package org.corespring.v2.auth.wired

import org.bson.types.ObjectId
import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.models.{ Standard, Subject }
import org.corespring.models.item.{ Item, FieldValue, PlayerDefinition }
import org.corespring.models.json.JsonFormatting
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.AuthMode.AuthMode
import org.corespring.v2.auth.models._
import org.corespring.v2.errors.Errors.{cannotLoadSessionCount, noItemIdInSession, cantLoadSession, generalError}
import org.corespring.v2.errors.V2Error
import org.corespring.v2.sessiondb.{ SessionServices, SessionService }
import org.joda.time.DateTime
import org.mockito.ArgumentMatcher
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsObject, Json, JsValue }

import scalaz.{ Failure, Validation, Success }

import scalaz.Scalaz._

class SessionAuthWiredTest extends Specification with Mockito with MockFactory {

  implicit val identity: OrgAndOpts = mockOrgAndOpts(AuthMode.AccessToken)

  val defaultItemFailure = generalError("no item")

  val defaultPlayerDefinition = PlayerDefinition.empty

  "SessionAuth" should {

    case class authScope(
      session: Option[JsValue] = None,
      playerDefinition: PlayerDefinition = defaultPlayerDefinition,
      itemLoadForRead: Boolean = true,
      itemLoadForWrite: Boolean = true,
      hasPerms: Validation[V2Error, Boolean] = Success(true),
      savedId: ObjectId = new ObjectId()) extends Scope {

      lazy val itemTransformer: ItemTransformer = {
        val m = mock[ItemTransformer]
        m.createPlayerDefinition(any[Item]) returns playerDefinition
        m
      }

      lazy val jsonFormatting = new JsonFormatting {
        override def fieldValue: FieldValue = ???

        override def findStandardByDotNotation: (String) => Option[Standard] = ???

        override def rootOrgId: ObjectId = ???

        override def findSubjectById: (ObjectId) => Option[Subject] = ???
      }

      lazy val mockCollectionId = ObjectId.get.toString

      class IsEmptyString() extends ArgumentMatcher[String] {
        override def matches(s: Any): Boolean = {
          s.isInstanceOf[String] && s.asInstanceOf[String].isEmpty
        }
      }

      lazy val itemAuth: ItemAuth[OrgAndOpts] = {
        val m = mock[ItemAuth[OrgAndOpts]] //.verbose
        val out = Success(
          Item(collectionId = mockCollectionId, playerDefinition = Some(playerDefinition)))

        m.loadForRead(anArgThat(new IsEmptyString()))(any[OrgAndOpts]) returns {
          //println("---> empty string")
          Failure(defaultItemFailure)
        }
        m.loadForRead(anyString)(any[OrgAndOpts]) returns (if (itemLoadForRead) out else Failure(defaultItemFailure))
        m.loadForWrite(anArgThat(new IsEmptyString()))(any[OrgAndOpts]) returns {
          //println("---> empty string")
          Failure(defaultItemFailure)
        }
        m.loadForWrite(anyString)(any[OrgAndOpts]) returns (if (itemLoadForWrite) out else Failure(defaultItemFailure))
        m
      }

      private def serviceMock(key: String) = {
        val m = mock[SessionService]
        m.load(anyString) returns session.map(s => Json.obj("service" -> key) ++ s.as[JsObject])
        m.create(any[JsValue]) returns Some(savedId)
        m.save(anyString, any[JsValue]).answers { (args, value) =>
          {
            Some(args.asInstanceOf[Array[Object]](1).asInstanceOf[JsValue])
          }
        }
        m
      }

      val sessionServices = SessionServices(serviceMock("preview"), serviceMock("main"))

      val perms = new HasPermissions {
        override def has(itemId: String, sessionId: Option[String], settings: PlayerAccessSettings): Validation[V2Error, Boolean] = Success(true)
      }

      val auth = new SessionAuthWired(
        itemTransformer,
        jsonFormatting,
        itemAuth,
        sessionServices,
        perms)
    }

    def getEmptyPlayerDefinition: PlayerDefinition = PlayerDefinition(
      Seq.empty,
      "",
      Json.obj(),
      "",
      None)

    "can create" should {
      "fail" in new authScope(itemLoadForRead = false) {
        auth.canCreate("") must_== Failure(defaultItemFailure)
      }

      "succeed" in new authScope() {
        auth.canCreate("") must_== Success(true)
      }
    }

    def run(fn: (SessionAuthWired) => Validation[V2Error, (JsValue, PlayerDefinition)], serviceName: String) = {
      "fail if there is no session" in new authScope() {
        fn(auth) must_== Failure(cantLoadSession(""))
      }

      "fail if there is a session with no item id" in new authScope(session = Some(Json.obj())) {
        fn(auth) must_== Failure(noItemIdInSession(""))
      }

      "fail if there is no item" in new authScope(itemLoadForRead = false, itemLoadForWrite = false, session = Some(Json.obj("itemId" -> "itemId"))) {
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
        session = Some(Json.obj("itemId" -> "itemId"))) {
        val Success((rSession, rItem)) = fn(auth)
        (rSession.as[JsObject] - "service", rItem) must_== (session.get, PlayerDefinition.empty)
        (rSession \ "service").as[String] must_== serviceName
      }
    }

    def runSave(fn: (SessionAuthWired) => Validation[V2Error, JsValue], serviceName: String) = {
      "fail if there is no session" in new authScope() {
        fn(auth) must_== Failure(cantLoadSession(""))
      }

      "find the definition in the session" in new authScope(session = Some(
        Json.obj("item" -> Json.obj(
          "xhtml" -> "<h1>Hello World</h1>",
          "components" -> Json.obj())))) {

        val Success(rSession) = fn(auth)
        (rSession \ "item").asOpt[JsObject] must_== None
      }

      "succeed" in new authScope(
        playerDefinition = getEmptyPlayerDefinition,
        session = Some(Json.obj("itemId" -> "itemId"))) {
        val Success(rSession) = fn(auth)
        rSession.as[JsObject] - "service" must_== session.get
        (rSession \ "service").as[String] must_== serviceName
      }
    }

    def opts(m: AuthMode, clientId: Option[String] = None) = OrgAndOpts(mockOrg(), PlayerAccessSettings.ANYTHING, m, clientId)

    "load for save - user session - uses preview service" should {
      runSave(b => b.loadForSave("")(opts(AuthMode.UserSession)), "preview")
    }

    "load for save - access token - uses main service" should {
      runSave(b => b.loadForSave("")(opts(AuthMode.AccessToken)), "main")
    }

    "load for save - access token - uses main service" should {
      runSave(b => b.loadForSave("")(opts(AuthMode.ClientIdAndPlayerToken)), "main")
    }

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
        session = Some(Json.obj("itemId" -> "itemId")),
        itemLoadForWrite = false) {
        val Success((rSession, playerDef)) = auth.loadForWrite("?")
        (rSession.as[JsObject] - "service", playerDef) must_== (session.get, PlayerDefinition.empty)
      }
    }

    "when saving" should {
      "add the identity data to the session data" in new authScope() {
        val optsIn = opts(AuthMode.ClientIdAndPlayerToken, Some("1"))
        val saveFn = auth.saveSessionFunction(optsIn)
        saveFn.toOption.map(fn => fn("1", Json.obj()))
        there was one(sessionServices.main).save("1", Json.obj("identity" -> IdentityJson(optsIn)))
      }

      "add the identity but not the apiClient id" in new authScope() {
        val optsIn = opts(AuthMode.AccessToken)
        val saveFn = auth.saveSessionFunction(optsIn)
        saveFn.toOption.map(fn => fn("1", Json.obj()))
        val identityJson = IdentityJson(optsIn)
        (identityJson \ "apiClientId").asOpt[String] must beNone
        there was one(sessionServices.main).save("1", Json.obj("identity" -> identityJson))
      }
    }

    "when creating" should {
      "add the identity data to the session data" in new authScope() {
        val optsIn = opts(AuthMode.ClientIdAndPlayerToken, Some("1"))
        auth.create(Json.obj())(optsIn)
        val captor = capture[JsValue]
        there was one(sessionServices.main).create(captor.capture)
        (captor.value \ "identity").as[JsObject] must_== IdentityJson(optsIn)
      }

      "add dateCreated to the session data" in new authScope() {
        val optsIn = opts(AuthMode.ClientIdAndPlayerToken, Some("1"))
        auth.create(Json.obj())(optsIn)
        val captor = capture[JsValue]
        there was one(sessionServices.main).create(captor.capture)
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
        there was one(sessionServices.main).load(any[String])
      }

      "return session with 0 attempts" in new authScope(session = Some(Json.obj()), savedId = new ObjectId()) {
        auth.reopen("")(optsIn) match {
          case Success(json) => (json \ "attempts").as[Int] must be equalTo (0)
          case _ => Failure("reopen was unsuccessful")
        }
      }

      "return session with isComplete false" in new authScope(session = Some(Json.obj()), savedId = new ObjectId()) {
        auth.reopen("")(optsIn) match {
          case Success(json) => (json \ "isComplete").as[Boolean] must beFalse
          case _ => Failure("reopen was unsuccessful")
        }
      }

    }

    "complete" should {
      val optsIn = opts(AuthMode.ClientIdAndPlayerToken, Some("1"))

      "save into main service" in new authScope(session = Some(Json.obj())) {
        auth.complete("")(optsIn)
        there was one(sessionServices.main).load(any[String])
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
        there was one(sessionServices.main).load(any[String])
      }

      "save into preview service" in new authScope(session = Some(Json.obj())) {
        auth.cloneIntoPreview("")(optsIn)
        there was one(sessionServices.preview).create(any[JsObject])
      }

      "return id of saved session" in new authScope(session = Some(Json.obj()), savedId = new ObjectId()) {
        auth.cloneIntoPreview("")(optsIn) must be equalTo (Success(savedId))
      }

    }

    "orgCount" should {

      val orgId = new ObjectId()
      val month = new DateTime()

      val optsIn = opts(AuthMode.ClientIdAndPlayerToken, Some("1"))

      val results = {
        val r = scala.util.Random
        1.to(31).map(day => new DateTime().withMonthOfYear(1).withDayOfMonth(day) -> r.nextInt(100).toLong).toMap
      }

      "SessionService returns successfully" should {

        trait SessionServiceSucceeds extends authScope {
          sessionServices.main.orgCount(any[ObjectId], any[DateTime]) returns Some(results)
        }

        "return result from SessionService" in new SessionServiceSucceeds() {
          auth.orgCount(orgId, month)(optsIn) must be equalTo(Success(results))
        }

      }

      "SessionService returns empty" should {
        trait SessionServiceEmpty extends authScope {
          sessionServices.main.orgCount(any[ObjectId], any[DateTime]) returns None
        }

        "return failure" in new SessionServiceEmpty {
          auth.orgCount(orgId, month)(optsIn) must be equalTo(Failure(cannotLoadSessionCount(orgId, month)))
        }
      }

    }

  }
}

