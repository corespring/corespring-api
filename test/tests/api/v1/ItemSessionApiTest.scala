package tests.api.v1

import models.ItemSessionSettings
import play.api.mvc.{Call, AnyContent}
import models.{ItemResponse, ItemSession}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import org.specs2.mutable._
import play.api.test.Helpers._
import tests.PlaySingleton
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

class ItemSessionApiTest extends Specification {

  PlaySingleton.start()

  val Routes = api.v1.routes.ItemSessionApi

  lazy val FakeAuthHeader = FakeHeaders(Map("Authorization" -> Seq("Bearer " + token)))

  val token = "34dj45a769j4e1c0h4wb"

  object IDs {
    val Item: String = "50083ba9e4b071cb5ef79101"
    val ItemSession: String = "502d0f823004deb7f4f53be7"
  }

  def invokeCall(makeCallFn: (() => Call), makeContent: (() => AnyContent), args: (String, String)*): ItemSession = {
    val call = makeCallFn()

    val url = call.url + "?" + args.toList.map((a: (String, String)) => a._1 + "=" + a._2).mkString("&")
    println("calling: " + call.method + " " + url)
    val request = FakeRequest(
      call.method,
      url,
      FakeAuthHeader,
      makeContent())

    val result = routeAndCall(request).get
    val json: JsValue = Json.parse(contentAsString(result))
    Json.fromJson[ItemSession](json)
  }

  def createNewSession(itemId: String = IDs.Item, content: AnyContent = AnyContentAsEmpty): ItemSession = {
    invokeCall(
      () => Routes.create(new ObjectId(itemId)),
      () => content
    )
  }

  def get(sessionId : String, itemId : String) : ItemSession = {
    invokeCall( () => Routes.get(new ObjectId(itemId), new ObjectId(sessionId)), () => AnyContentAsEmpty  )
  }

  def processResponse(session: ItemSession): ItemSession = {
    invokeCall(
      () => Routes.update(session.itemId, session.id),
      () => AnyContentAsJson(Json.toJson(session))
    )
  }

  def update(session: ItemSession): ItemSession = {
    invokeCall(
      () => Routes.update(session.itemId, session.id),
      () => AnyContentAsJson(Json.toJson(session)),
      ("action", "updateSettings")
    )
  }

  def begin(s: ItemSession) = {
    invokeCall(
      () => Routes.update(s.itemId, s.id),
      () => AnyContentAsJson(Json.toJson(s)),
      ("action", "begin")
    )
  }


  "item session data" should {

    "return an error if we try and update an item that is already finished" in {

      val newSession: ItemSession = createNewSession()

      val testSession = ItemSession(itemId = new ObjectId())

      //testSession.id = new ObjectId(testSessionIds("itemSessionId"))
      testSession.responses = testSession.responses ++ Seq(ItemResponse("mexicanPresident", "calderon", "{$score:1}"))
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
    testSession.responses = testSession.responses ++ Seq(ItemResponse("mexicanPresident", "calderon", "{$score:1}"))
    testSession.responses = testSession.responses ++ Seq(ItemResponse("irishPresident", "guinness", "{$score:0}"))
    testSession.responses = testSession.responses ++ Seq(ItemResponse("winterDiscontent", "York", "{$score:1}"))
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
    val optQtiItem: Either[InternalError, QtiItem] = ItemSession.getXmlWithFeedback(new ObjectId(IDs.Item), ItemSession.findOneById(newSession.id).get.feedbackIdLookup) match {
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


    "return an item session which contains feedback contents within sessionData which contains all feedback elements in the xml which correspond to responses from client" in {
      val json: JsValue = Json.parse(contentAsString(result))

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
      }
    }

    "return an item session which contains correctResponse object within sessionData which contains all correct responses available" in {
      val json: JsValue = Json.parse(contentAsString(result))
      (json \ "sessionData") match {
        case JsObject(sessionData) => sessionData.find(field => field._1 == "correctResponses") match {
          case Some((_, jscorrectResponses)) => jscorrectResponses match {
            case JsObject(correctResponses) =>
              if (correctResponses.foldRight[Boolean](true)((prop, acc) => {
                acc && (prop._1 match {
                  case "mexicanPresident" => prop._2.as[String] == "calderon"
                  case "irishPresident" => prop._2.as[String] == "higgins"
                  case "rainbowColors" => prop._2.as[Seq[String]].sameElements(Seq("blue", "violet", "red"))
                  case "winterDiscontent" => prop._2.as[Seq[String]].sameElements(Seq("York", "york"))
                  case "wivesOfHenry" => prop._2.as[Seq[String]].equals(Seq("aragon", "boleyn", "seymour", "cleves", "howard", "parr"))
                  case "cutePugs" => prop._2.as[Seq[String]].equals(Seq("pug1", "pug2", "pug3"))
                  case "manOnMoon" => prop._2.as[String] == "armstrong"
                  case _ => false
                })
              })) success
              else failure
            case _ => failure
          }
          case _ => failure
        }
        case _ => failure
      }
    }

    "support item creation " in {

      val testSession = ItemSession(new ObjectId(IDs.Item))
      testSession.responses = testSession.responses ++ Seq(ItemResponse("mexicanPresident", "calderon", "{$score:1}"))
      testSession.responses = testSession.responses ++ Seq(ItemResponse("irishPresident", "guinness", "{$score:0}"))
      testSession.responses = testSession.responses ++ Seq(ItemResponse("winterDiscontent", "York", "{$score:1}"))
      val newSession = createNewSession(IDs.Item, AnyContentAsJson(Json.toJson(testSession)))
      val unequalItems = getUnequalItems(newSession,testSession)
      unequalItems must be(Seq())
    }

    "support retrieval of an itemsession" in {

      val dbSession = ItemSession.findOneById( new ObjectId(IDs.ItemSession)).get
      val session = get( IDs.ItemSession, IDs.Item)
      dbSession.id must equalTo(session.id)
      val unequalItems = getUnequalItems(dbSession, session)
      unequalItems must be(Seq())
      success
    }
  }


  private def getUnequalItems(a:ItemSession, b: ItemSession): Seq[String] = {

    val out = Seq[Option[String]](
      if(a.itemId equals b.itemId) None else Some("itemId"),
      if(a.start equals b.start) None else Some("start"),
      if(a.finish equals b.finish) None else Some("finish")
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
