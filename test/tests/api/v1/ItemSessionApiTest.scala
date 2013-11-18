package tests.api.v1

import api.v1.ItemSessionApi
import org.bson.types.ObjectId
import org.corespring.platform.core.models.error.InternalError
import org.corespring.platform.core.models.itemSession._
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qti.models.QtiItem
import org.corespring.qti.models.responses.ArrayResponse
import org.corespring.qti.models.responses.Response
import org.corespring.qti.models.responses.StringResponse
import org.corespring.test.BaseTest
import org.corespring.test.utils.RequestCalling
import org.joda.time.DateTime
import org.specs2.mutable._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.mvc.AnyContentAsJson
import play.api.mvc._
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.Left
import scala.Right
import scala.Some
import api.ApiError

class ItemSessionApiTest extends BaseTest with RequestCalling {

  sequential

  val Routes = api.v1.routes.ItemSessionApi
  val Api = api.v1.ItemSessionApi

  object IDs {
    val Item: String = "511156d38604c9f77da9739d"
    val ItemSession: String = "51116bc7a14f7b657a083c1d"
  }

  def createNewSession(itemId: String = IDs.Item, content: AnyContent = AnyContentAsEmpty): ItemSession = {
    invokeCall[ItemSession](
      Api.create(versionedId(itemId)),
      content)
  }

  def get(sessionId: String, itemId: String): ItemSession = {
    val result = ItemSessionApi.get(versionedId(itemId), new ObjectId(sessionId), "student")(FakeRequest("", tokenize(""), FakeHeaders(), AnyContentAsEmpty))
    val json = Json.parse(contentAsString(result))
    json.as[ItemSession]
  }

  def processResponse(session: ItemSession): ItemSession = {
    invokeCall[ItemSession](
      Api.update(session.itemId, session.id, "student",None),
      AnyContentAsJson(Json.toJson(session)))
  }

  def update(session: ItemSession): ItemSession = {
    invokeCall[ItemSession](
      Api.update(session.itemId, session.id, "student", Some("updateSettings")),
      AnyContentAsJson(Json.toJson(session)))
  }

  def begin(s: ItemSession): ItemSession = {
    invokeCall[ItemSession](
      Api.update(s.itemId, s.id, "student", Some("begin")),
      AnyContentAsJson(Json.toJson(s)))
  }

  "item session data" should {

    "return an error if we try and update an item that is already finished" in {

      val newSession: ItemSession = createNewSession()

      val testSession = ItemSession(itemId = versionedId(ObjectId.get.toString))

      //testSession.id = new ObjectId(testSessionIds("itemSessionId"))
      testSession.responses = testSession.responses ++ Seq(StringResponse("mexicanPresident", "calderon"))
      testSession.finish = Some(new DateTime())

      val update = api.v1.routes.ItemSessionApi.update(versionedId(IDs.Item), newSession.id)

      val updateRequest = FakeRequest(
        update.method,
        update.url,
        FakeAuthHeader,
        AnyContentAsJson(Json.toJson(testSession)))

      //First update is fine
      route(updateRequest) match {
        case Some(result) => status(result) must equalTo(OK)
        case _ => failure("First update didn't work")
      }

      //This will fail because a finish has been set for this ItemSession in the previous request.
      route(updateRequest) match {
        case Some(result) => {
          status(result) must equalTo(BAD_REQUEST)
          val json = Json.parse(contentAsString(result))
          (json \ "message").asOpt[String] must equalTo(Some(ApiError.ItemSessionFinished.message))
          (json \ "code").asOpt[Int] must equalTo(Some(ApiError.ItemSessionFinished.code))
        }
        case _ => failure("Second update didn't work")
      }
    }
  }

  "process" should {

    "if it isn't an 'attempt' just save the responses" in {

      val settings = ItemSessionSettings(maxNoOfAttempts = 5)
      val session = ItemSession(
        itemId = versionedId(IDs.Item),
        settings = settings
      )
      val newSession = createNewSession(IDs.Item, AnyContentAsJson(Json.toJson(session)))
      val update = newSession.copy(responses = Seq(StringResponse("id", "blah")))
      val json = Json.toJson(update).as[JsObject] ++ Json.obj("isAttempt" -> false)
      val updatedSession = invokeCall[ItemSession]( Api.update(newSession.itemId, newSession.id, "student", None), AnyContentAsJson(json))
      updatedSession.responses.length === 1
    }


    "ignore any settings in the model" in {

      val settings = ItemSessionSettings(maxNoOfAttempts = 5)
      val session = ItemSession(itemId = versionedId(IDs.Item), settings = settings)
      val newSession = createNewSession(IDs.Item, AnyContentAsJson(Json.toJson(session)))
      newSession.settings.maxNoOfAttempts must equalTo(settings.maxNoOfAttempts)

      newSession.settings.maxNoOfAttempts = 10
      val processed = processResponse(newSession)
      processed.settings.maxNoOfAttempts must equalTo(settings.maxNoOfAttempts)
    }

    "return a finish after the first attempt if only one attempt is allowed" in {
      val settings = ItemSessionSettings(maxNoOfAttempts = 1)
      val session = ItemSession(itemId = versionedId(IDs.Item), settings = settings)
      val newSession = createNewSession(IDs.Item, AnyContentAsJson(Json.toJson(session)))
      val processed = processResponse(newSession)
      processed.finish must not beNone
    }
  }
  "creating" should {

    "accept a json body with an itemSession" in {

      val settings = ItemSessionSettings(maxNoOfAttempts = 12)
      val session = ItemSession(itemId = versionedId(IDs.Item), settings = settings)
      val json = Json.toJson(session)
      val newSession = createNewSession(IDs.Item, AnyContentAsJson(json))
      newSession.settings.maxNoOfAttempts must equalTo(12)
    }

    "only use the item id set in the url" in {
      val session = ItemSession(itemId = VersionedId(new ObjectId()))
      val newSession = createNewSession(IDs.Item, AnyContentAsJson(Json.toJson(session)))
      newSession.itemId must equalTo(versionedId(IDs.Item))
    }
  }

  "updating" should {
    "allow updates to settings" in {
      val newSession = createNewSession()
      val settings = ItemSessionSettings(submitCompleteMessage = "custom message")
      newSession.settings = settings
      val updatedSession = update(newSession)
      updatedSession.settings.submitCompleteMessage must equalTo("custom message")
    }
  }

  "begin" should {
    "set the start value in the session" in {
      val newSession = createNewSession()
      if (newSession.start.isDefined) failure else success
      val result = begin(newSession)
      if (result.start.isDefined) success else failure
    }
  }

  "creating and then updating item session" should {
    import org.corespring.platform.core.models.itemSession.{ DefaultItemSession => IS }

    def addResponses(): Seq[Response] = Seq(
      StringResponse("mexicanPresident", "calderon"),
      StringResponse("irishPresident", "guinness"),
      StringResponse("winterDiscontent", "York"))

    def optQtiItem(testSession: ItemSession): Either[InternalError, QtiItem] = IS.getXmlWithFeedback(IS.findOneById(testSession.id).get) match {
      case Right(elem) => Right(QtiItem(elem))
      case Left(e) => Left(e)
    }

    def process(s: ItemSession): JsValue = {
      val result = ItemSessionApi.processResponse(s.itemId, s.id)("student")(FakeRequest("", tokenize(""), FakeHeaders(), AnyContentAsJson(Json.toJson(s))))
      //logger.debug(s"process : : : ${contentAsString(result)}")
      Json.parse(contentAsString(result))
    }

    "create a cached qti xml for the specified item" in {
      val s = createNewSession()
      optQtiItem(s) must beRight[QtiItem]
    }

    "return an item session which contains a sessionData property" in {
      val s = createNewSession().copy(responses = addResponses())
      //Note: We can't use the process method in the test because when reading the json it will strip 'sessionData'
      val json = process(s)
      (json \ "sessionData" \ "correctResponses")(0) === JsObject(Seq("id" -> JsString("mexicanPresident"), "value" -> JsString("calderon")))
    }

    "return an item session feedback contents with sessionData which contains all feedback elements in the xml which correspond to responses from client" in {

      val testSession = createNewSession().copy(responses = Seq(StringResponse("winterDiscontent", "York")))
      //logger.debug(s" test session : $testSession")
      val json = process(testSession)
      (json \ "sessionData" \ "feedbackContents").as[JsObject].keys.size === 1
    }

    "return an item session which contains correctResponse object within sessionData which contains all correct responses available" in {

      val s = createNewSession().copy(responses = addResponses())
      val correctResponses = (process(s) \ "sessionData" \ "correctResponses").as[Seq[Response]]

      def sir(a: String, b: String): Response = StringResponse(a, b)
      def air(a: String, b: Seq[String]): Response = ArrayResponse(a, b)

      val expectedValues = Seq(
        sir("mexicanPresident", "calderon"),
        sir("irishPresident", "higgins"),
        air("rainbowColors", Seq("blue", "violet", "red")),
        air("winterDiscontent", Seq("York", "york")),
        air("wivesOfHenry", Seq("aragon", "boleyn", "seymour", "cleves", "howard", "parr")),
        air("cutePugs", Seq("pug1", "pug2", "pug3")),
        sir("manOnMoon", "armstrong"))
      correctResponses === expectedValues
    }

    "support retrieval of an itemsession" in {
      val dbSession = DefaultItemSession.findOneById(new ObjectId(IDs.ItemSession)).get
      val session = get(IDs.ItemSession, IDs.Item)
      dbSession.id === session.id
    }
  }

  "item session on get" should {
    "show correct responses even if settings says otherwise if there is render options with role=instructor and instructor role is requested" in {
      callSessionGet("instructor", """{"itemId":"*","sessionId":"*","assessmentId":"*","role":"instructor","expires":0,"mode":"render"}""",true)
    }
    "show correct responses even if settings says otherwise if there is render options with role=* and instructor role is requested" in {
      callSessionGet("instructor", """{"itemId":"*","sessionId":"*","assessmentId":"*","role":"*","expires":0,"mode":"render"}""",true)
    }
    "show correct responses even if settings says otherwise if there is render options with role=instructor and student role is requested" in {
      callSessionGet("student", """{"itemId":"*","sessionId":"*","assessmentId":"*","role":"instructor","expires":0,"mode":"render"}""",false)
    }
    "show correct responses even if settings says otherwise if there is render options with role=* and student role is requested" in {
      callSessionGet("student", """{"itemId":"*","sessionId":"*","assessmentId":"*","role":"*","expires":0,"mode":"render"}""",false)
    }
  }

  "item session on update" should {
    "show correct responses even if settings says otherwise if there is render options with role=instructor and instructor role is requested" in {
      callSessionUpdate("instructor", """{"itemId":"*","sessionId":"*","assessmentId":"*","role":"instructor","expires":0,"mode":"render"}""",true)
    }
    "show correct responses even if settings says otherwise if there is render options with role='*' and instructor role is requested" in {
      callSessionUpdate("instructor", """{"itemId":"*","sessionId":"*","assessmentId":"*","role":"*","expires":0,"mode":"render"}""",true)
    }
    "do not show correct responses according to settings if there is render options with role=instructor and student role is requested" in {
      callSessionUpdate("student", """{"itemId":"*","sessionId":"*","assessmentId":"*","role":"instructor","expires":0,"mode":"render"}""",false)
    }
    "do not show correct responses according to settings if there is render options with role=* and student role is requested" in {
      callSessionUpdate("student", """{"itemId":"*","sessionId":"*","assessmentId":"*","role":"*","expires":0,"mode":"render"}""",false)
    }
  }

  def callSessionGet(role:String, renderOptions: String, showCorrectResponses:Boolean) = {
    val s = DefaultItemSession.newSession(ItemSession(start = Some(new DateTime()), finish = Some(new DateTime()), itemId = versionedId(IDs.Item),
      settings = ItemSessionSettings(highlightCorrectResponse = false, highlightUserResponse = false, showFeedback = false),
      responses = Seq(StringResponse("mexicanPresident", "calderon"),StringResponse("irishPresident", "guinness"),StringResponse("winterDiscontent", "York"))
    )).right.get
    val get = api.v1.routes.ItemSessionApi.get(versionedId(IDs.Item), s.id, role)
    val request = FakeRequest(get.method, tokenize(get.url)).withSession(
      "player.renderOptions" -> renderOptions
    )
    matchSettingsAndSessionData(route(request).get,showCorrectResponses)
  }

  def callSessionUpdate(role:String, renderOptions: String, showCorrectResponses:Boolean) = {
    val s = createNewSession(content = AnyContentAsJson(Json.obj(
      "settings" -> Json.toJson(ItemSessionSettings(highlightCorrectResponse = false, highlightUserResponse = false, showFeedback = false))
    )))
    val update = api.v1.routes.ItemSessionApi.update(versionedId(IDs.Item), s.id, role)
    val request = FakeRequest(
      update.method,
      tokenize(update.url),
      FakeAuthHeader,
      AnyContentAsJson(Json.obj(
        "itemId" -> JsString(IDs.Item),
        "responses" -> Json.toJson(Seq(StringResponse("mexicanPresident", "calderon"),
          StringResponse("irishPresident", "guinness"), StringResponse("winterDiscontent", "York"))))))
      .withSession(
      "player.renderOptions" -> renderOptions
    )
    matchSettingsAndSessionData(route(request).get,showCorrectResponses)
  }

  def matchSettingsAndSessionData(result:Result, showCorrectResponses:Boolean) = {
    val jsItemSession = Json.parse(contentAsString(result))
    val itemSession = Json.fromJson[ItemSession](jsItemSession).get
    DefaultItemSession.remove(itemSession)
    (jsItemSession \ "sessionData") must beAnInstanceOf[JsObject]
    itemSession.settings.highlightCorrectResponse === showCorrectResponses
    itemSession.settings.highlightUserResponse === showCorrectResponses
    itemSession.settings.showFeedback === showCorrectResponses
  }


  "reopen" should {

    "reopen" in {
      val id = ObjectId.get
      val session = ItemSession(itemId = versionedId(IDs.Item), id = id, finish = Some(DateTime.now), attempts = 10)
      DefaultItemSession.insert(session)
      val action = Api.reopen(session.itemId, session.id)
      val updated = invokeCall[ItemSession](action, AnyContentAsEmpty)
      updated.isFinished === false
      updated.attempts === 0
    }
  }

  /**
   * Clean up any state created
   */
  trait cleanUp extends After {
    def after {
      // TODO - delete created sessions
    }
  }

}
