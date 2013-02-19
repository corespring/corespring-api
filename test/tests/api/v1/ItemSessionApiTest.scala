package tests.api.v1

import models.{StringItemResponse, ItemSessionSettings, ItemResponse, ItemSession}
import play.api.mvc.{Call, AnyContent}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Result
import play.api.test.FakeRequest
import org.specs2.mutable._
import play.api.test.Helpers._
import tests.{BaseTest, PlaySingleton}
import api.ApiError
import qti.models._
import scala.Left
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import scala.Some
import controllers.InternalError
import play.api.test.FakeHeaders
import scala.Right
import play.api.mvc.AnyContentAsJson
import play.api.libs.json.JsObject

class ItemSessionApiTest extends BaseTest {

  val Routes = api.v1.routes.ItemSessionApi

  lazy val FakeAuthHeader = FakeHeaders(Map("Authorization" -> Seq("Bearer " + token)))

  object IDs {
    val Item: String = "511156d38604c9f77da9739d"
    val ItemSession: String = "51116bc7a14f7b657a083c1d"
  }

  def invokeCall(call: Call, content: AnyContent, args: (String, String)*): ItemSession = {

    val url = call.url + "?" + args.toList.map((a: (String, String)) => a._1 + "=" + a._2).mkString("&")
    println("calling: " + call.method + " " + url)
    val request = FakeRequest(
      call.method,
      url,
      FakeAuthHeader,
      content)

    val result = routeAndCall(request).get
    val json: JsValue = Json.parse(contentAsString(result))
    Json.fromJson[ItemSession](json)
  }

  def createNewSession(itemId: String = IDs.Item, content: AnyContent = AnyContentAsEmpty): ItemSession = {
    invokeCall(
      Routes.create(new ObjectId(itemId)),
      content
    )
  }

  def get(sessionId: String, itemId: String): ItemSession = {
    invokeCall(Routes.get(new ObjectId(itemId), new ObjectId(sessionId)), AnyContentAsEmpty)
  }

  def processResponse(session: ItemSession): ItemSession = {
    invokeCall(
      Routes.update(session.itemId, session.id),
      AnyContentAsJson(Json.toJson(session))
    )
  }

  def update(session: ItemSession): ItemSession = {
    invokeCall(
      Routes.update(session.itemId, session.id),
      AnyContentAsJson(Json.toJson(session)),
      ("action", "updateSettings")
    )
  }

  def begin(s: ItemSession) = {
    invokeCall(
      Routes.update(s.itemId, s.id),
      AnyContentAsJson(Json.toJson(s)),
      ("action", "begin")
    )
  }


  "item session data" should {

    "return an error if we try and update an item that is already finished" in {

      val newSession: ItemSession = createNewSession()

      val testSession = ItemSession(itemId = new ObjectId())

      //testSession.id = new ObjectId(testSessionIds("itemSessionId"))
      testSession.responses = testSession.responses ++ Seq(StringItemResponse("mexicanPresident", "calderon"))
      testSession.finish = Some(new DateTime())

      val update = api.v1.routes.ItemSessionApi.update(new ObjectId(IDs.Item), newSession.id)

      val updateRequest = FakeRequest(
        update.method,
        update.url,
        FakeAuthHeader,
        AnyContentAsJson(Json.toJson(testSession))
      )

      //First update is fine
      routeAndCall(updateRequest) match {
        case Some(result) => status(result) must equalTo(OK)
        case _ => failure("First update didn't work")
      }

      //This will fail because a finish has been set for this ItemSession in the previous request.
      routeAndCall(updateRequest) match {
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
    "ignore any settings in the model" in {

      val settings = ItemSessionSettings(maxNoOfAttempts = 5)
      val session = ItemSession(itemId = new ObjectId(IDs.Item), settings = settings)
      val newSession = createNewSession(IDs.Item, AnyContentAsJson(Json.toJson(session)))
      newSession.settings.maxNoOfAttempts must equalTo(settings.maxNoOfAttempts)

      newSession.settings.maxNoOfAttempts = 10
      val processed = processResponse(newSession)
      processed.settings.maxNoOfAttempts must equalTo(settings.maxNoOfAttempts)
    }

    "return a finish after the first attempt if only one attempt is allowed" in {
      val settings = ItemSessionSettings(maxNoOfAttempts = 1)
      val session = ItemSession(itemId = new ObjectId(IDs.Item), settings = settings)
      val newSession = createNewSession(IDs.Item, AnyContentAsJson(Json.toJson(session)))
      val processed = processResponse(newSession)
      processed.finish must not beNone
    }
  }

  "creating" should {

    "accept a json body with an itemSession" in {

      val settings = ItemSessionSettings(maxNoOfAttempts = 12)
      val session = ItemSession(itemId = new ObjectId(IDs.Item), settings = settings)
      val json = Json.toJson(session)
      val newSession = createNewSession(IDs.Item, AnyContentAsJson(json))
      newSession.settings.maxNoOfAttempts must equalTo(12)
    }

    "only use the item id set in the url" in {
      val session = ItemSession(itemId = new ObjectId())
      val newSession = createNewSession(IDs.Item, AnyContentAsJson(Json.toJson(session)))
      newSession.itemId.toString must equalTo(IDs.Item)
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
      println(".. begin..")
      println(result)
      if (result.start.isDefined) success else failure
    }
  }

  "creating and then updating item session" should {
    val newSession = createNewSession()
    val updateCall = api.v1.routes.ItemSessionApi.update(new ObjectId(IDs.Item), newSession.id)
    val testSession = ItemSession(itemId = new ObjectId(IDs.Item))
    // add some item responses
    testSession.responses = testSession.responses ++ Seq(StringItemResponse("mexicanPresident", "calderon"))
    testSession.responses = testSession.responses ++ Seq(StringItemResponse("irishPresident", "guinness"))
    testSession.responses = testSession.responses ++ Seq(StringItemResponse("winterDiscontent", "York"))
    testSession.finish = Some(new DateTime())

    val json = Json.toJson(testSession)
    println(Json.stringify(json))

    val getRequest = FakeRequest(
      updateCall.method,
      updateCall.url,
      FakeAuthHeader,
      AnyContentAsJson(json)
    )
    val result = routeAndCall(getRequest).get
    ItemSession.remove(newSession)
    val optQtiItem: Either[InternalError, QtiItem] = ItemSession.getXmlWithFeedback( ItemSession.findOneById(newSession.id).get) match {
      case Right(elem) => Right(QtiItem(elem))
      case Left(e) => Left(e)
    }
    "create a cached qti xml for the specified item" in {
      optQtiItem must beRight[QtiItem]
    }


    "return an item session which contains a sessionData property" in {
      val json: JsValue = Json.parse(contentAsString(result))
      (json \ "sessionData") match {
        case JsObject(sessionData) => success
        case _ => failure
      }
    }

    def getFeedbackContents(result: Result): Option[Seq[(String,JsValue)]] = {

      val json: JsValue = Json.parse(contentAsString(result))

      (json \ "sessionData") match {
        case JsObject(sd) => {
          sd.find(_._1 == "feedbackContents") match {
            case Some((_, contents)) => contents match {
              case JsObject(c) => Some(c)
              case _ => None
            }
            case _ => None
          }
        }
        case _ => None
      }
    }

    "return an item session feedback contents with sessionData which contains all feedback elements in the xml which correspond to responses from client" in {

      val newSession = createNewSession()
      val updateCall = api.v1.routes.ItemSessionApi.update(new ObjectId(IDs.Item), newSession.id)
      val testSession = ItemSession(itemId = new ObjectId(IDs.Item))
      // add some item responses
      testSession.responses = testSession.responses ++ Seq(StringItemResponse("winterDiscontent", "York"))
      testSession.finish = Some(new DateTime())

      val getRequest = FakeRequest(
        updateCall.method,
        updateCall.url,
        FakeAuthHeader,
        AnyContentAsJson(json)
      )
      val result = routeAndCall(getRequest).get

      getFeedbackContents(result) match {
        case Some(seq) => {
          println("found feedbackContents: ")
          println(seq)
          success
        }
        case _ => failure("couldn't find contents")
      }

      /*val json: JsValue = Json.parse(contentAsString(result))

      (json \ "sessionData") match {
        case JsObject(sessionData) => sessionData.find(field => field._1 == "feedbackContents") match {
          case Some((_, jsfeedbackContents)) => jsfeedbackContents match {
            case JsObject(feedbackContents) => optQtiItem match {
              case Right(qtiItem) =>
                val feedbackBlocks = qtiItem.itemBody.feedbackBlocks
                val feedbackInlines = qtiItem.itemBody.interactions.map(i => i match {
                  case ChoiceInteraction(_, choices) => choices.map(choice => choice.feedbackInline)
                  case OrderInteraction(_, choices) => choices.map(choice => choice.feedbackInline)
                  case InlineChoiceInteraction(_, choices) => choices.map(choice => choice.feedbackInline)
                  case TextEntryInteraction(_, _, blocks) => List(blocks)
                  case _ => throw new RuntimeException("unknown interaction")
                }).flatten.flatten ++ feedbackBlocks

                def feedbackInlineContent(fi: FeedbackInline) = if (fi.defaultFeedback) fi.defaultContent(qtiItem) else fi.content

                def valueContains(feedbackInline: Option[FeedbackInline], value: JsValue): Boolean = feedbackInline match {
                  case Some(fb) => {
                    val fbContent = feedbackInlineContent(fb)

                    value match {
                      case JsArray(values) => values.contains(JsString(fbContent))
                      case JsString(value) => value == fbContent
                      case _ => false
                    }
                  }
                  case _ => false
                }


                def allFeedbackItemsArePresent(contents: Seq[(String, JsValue)], feedbacks: Seq[FeedbackInline]) = {
                  contents.foldRight[Boolean](true)((field, acc) => {

                    val fieldId = field._1
                    val fieldValue = field._2

                    if (acc) {
                      val maybeFeedbackInline = feedbacks.find(fieldId == _.csFeedbackId)
                      valueContains(maybeFeedbackInline, fieldValue)
                    } else {
                      false
                    }
                  })
                }

                if (allFeedbackItemsArePresent(feedbackContents, feedbackInlines)) success else failure

              case Left(_) => failure
            }
            case _ => failure
          }
          case _ => failure
        }
        case _ => failure
      }*/
    }

    def getCorrectResponses(result: Result): Seq[JsValue] = {
      val jsonString = contentAsString(result)
      val json: JsValue = Json.parse(jsonString)
      (json \ "sessionData") match {
        case JsObject(sessionData) => sessionData.find(field => field._1 == "correctResponses") match {
          case Some((_, jscorrectResponses)) => jscorrectResponses match {
            case JsArray(correctResponses) => correctResponses
            case _ => throw new RuntimeException("no array found")
          }
          case _ => throw new RuntimeException("no field called correctResponses")
        }
        case _ => throw new RuntimeException("no sessionData found")
      }
    }

    "return an item session which contains correctResponse object within sessionData which contains all correct responses available" in {
      val correctResponses = getCorrectResponses(result)

      def _jso(id: String, value: JsValue): JsObject = JsObject(Seq("id" -> JsString(id), "value" -> value))
      def _jsa(s: Seq[String]): JsArray = JsArray(s.map(JsString(_)))

      val expectedJsValues = Seq(
        _jso("mexicanPresident", JsString("calderon")),
        _jso("irishPresident", JsString("higgins")),
        _jso("rainbowColors", _jsa(Seq("blue", "violet", "red"))),
        _jso("winterDiscontent", _jsa(Seq("York", "york"))),
        _jso("wivesOfHenry", _jsa(Seq("aragon", "boleyn", "seymour", "cleves", "howard", "parr"))),
        _jso("cutePugs", _jsa(Seq("pug1", "pug2", "pug3"))),
        _jso("manOnMoon", JsString("armstrong"))
      )
      def jsString(value: JsValue) = Json.stringify(value)
      val expected = expectedJsValues.map(jsString).mkString("\n")
      val actual = correctResponses.map(jsString).mkString("\n")
      println(actual)
      expected must equalTo(actual)
    }

    "support item creation " in {
      val testSession = ItemSession(new ObjectId(IDs.Item))
      val newSession = createNewSession(IDs.Item, AnyContentAsJson(Json.toJson(testSession)))
      val unequalItems = getUnequalItems(newSession, testSession)
      unequalItems must be(Seq())
    }

    "support retrieval of an itemsession" in {
      val dbSession = ItemSession.findOneById(new ObjectId(IDs.ItemSession)).get
      val session = get(IDs.ItemSession, IDs.Item)
      dbSession.id must equalTo(session.id)
      val unequalItems = getUnequalItems(dbSession, session)
      unequalItems must be(Seq())
      success
    }
  }


  private def getUnequalItems(a: ItemSession, b: ItemSession): Seq[String] = {

    val out = Seq[Option[String]](
      if (a.itemId equals b.itemId) None else Some("itemId"),
      if (a.start equals b.start) None else Some("start"),
      if (a.finish equals b.finish) None else Some("finish")
    )
    out.flatten
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
