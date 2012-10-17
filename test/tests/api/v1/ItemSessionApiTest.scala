package tests.api.v1

import models.{SessionData, ItemResponse, ItemSession, Item}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.specs2.execute.Pending
import play.api.libs.json._
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsJson}
import play.api.test.{FakeHeaders, FakeRequest}
import org.specs2.mutable._
import play.api.test.Helpers._
import scala.Some
import play.api.test.FakeHeaders
import scala.Some
import tests.PlaySingleton
import play.api.test.FakeHeaders
import scala.Some
import play.api.mvc.AnyContentAsJson
import play.api.libs.json.JsObject
import api.ApiError
import controllers.InternalError
import controllers.testplayer.qti._

/**
 * Tests the ItemSession model
 */
class ItemSessionApiTest extends Specification {

  PlaySingleton.start()

  // from standard fixture data
  val token = "34dj45a769j4e1c0h4wb"
  val testItemId = "50083ba9e4b071cb5ef79101"

  val testSessionIds = Map(
    "itemId" -> "50083ba9e4b071cb5ef79101",
    "itemSessionId" -> "502d0f823004deb7f4f53be7"
  )

  def createNewSession () : ItemSession = {

    val call = api.v1.routes.ItemSessionApi.createItemSession(new ObjectId(testSessionIds("itemId")))

    val newSessionRequest = FakeRequest(
      call.method,
      call.url,
      FakeHeaders(Map("Authorization" -> Seq("Bearer "+token))),
      AnyContentAsEmpty
    )

    val newSessionResult = routeAndCall(newSessionRequest).get
    val newSessionJson:JsValue = Json.parse(contentAsString(newSessionResult))
    Json.fromJson[ItemSession](newSessionJson)
  }

  // create test session bound to random object id
  // in practice ItemSessions need to be bound to an item
  val testSession = ItemSession(new ObjectId())

  "ItemSession" should {
    "be saveable" in {
      ItemSession.save(testSession)
      ItemSession.findOneById(testSession.id) match {
        case Some(result) => success
        case _ => failure
      }
    }

    "be deletable" in {
      ItemSession.remove(testSession)
      ItemSession.findOneById(testSession.id) match {
        case Some(result) => failure
        case _ => success
      }
    }
  }


  /**
   * TODO - implement these tests...
   */
  "item session data" should {

    "return an error if we try and update an item that is already finished" in {

      val newSession : ItemSession = createNewSession()

      //testSession.id = new ObjectId(testSessionIds("itemSessionId"))
      testSession.responses = testSession.responses ++ Seq(ItemResponse("mexicanPresident", "calderon", "{$score:1}"))
      testSession.finish = Some(new DateTime())

      val itemId = testSessionIds("itemId")
      val update = api.v1.routes.ItemSessionApi.updateItemSession(new ObjectId(itemId), newSession.id)

      val updateRequest = FakeRequest(
        update.method,
        update.url,
        FakeHeaders(Map("Authorization" -> Seq("Bearer " + token))),
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
          (json \ "message" ).asOpt[String] must equalTo( Some(ApiError.ItemSessionFinished.message ))
          (json \ "code" ).asOpt[Int] must equalTo( Some(ApiError.ItemSessionFinished.code))
        }
        case _ => failure("Second update didn't work")
      }
    }
  }

  "creating and then updating item session" should {
    val newSession = createNewSession()
    val url = "/api/v1/items/" + testSessionIds("itemId") + "/sessions/" + newSession.id.toString
    val testSession = ItemSession(new ObjectId(testSessionIds("itemId")))
    // add some item responses
    testSession.responses = testSession.responses ++ Seq(ItemResponse("mexicanPresident", "calderon", "{$score:1}"))
    testSession.responses = testSession.responses ++ Seq(ItemResponse("irishPresident", "guinness", "{$score:0}"))
    testSession.responses = testSession.responses ++ Seq(ItemResponse("winterDiscontent", "York", "{$score:1}"))
    testSession.finish = Some(new DateTime())
    val getRequest = FakeRequest(
      PUT,
      url,
      FakeHeaders(Map("Authorization" -> Seq("Bearer " + token))),
      AnyContentAsJson(Json.toJson(testSession))
    )
    val result = routeAndCall(getRequest).get
    ItemSession.remove(newSession)
    val optQtiItem:Either[InternalError,QtiItem] = ItemSession.getXmlWithFeedback(new ObjectId(testSessionIds("itemId")),ItemSession.findOneById(newSession.id).get.feedbackIdLookup) match {
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
          case Some((_,jsfeedbackContents)) => jsfeedbackContents match {
            case JsObject(feedbackContents) => optQtiItem match {
              case Right(qtiItem) =>
                val feedbackBlocks = qtiItem.itemBody.feedbackBlocks
                val feedbackInlines = qtiItem.itemBody.interactions.map(i => i match {
                  case ChoiceInteraction(_,choices) => choices.map(choice => choice.feedbackInline)
                  case OrderInteraction(_,choices) => choices.map(choice => choice.feedbackInline)
                  case _ => throw new RuntimeException("unknown interaction")
                }).flatten.flatten ++ feedbackBlocks
                def feedbackInlineContent(fi:FeedbackInline) = if (fi.defaultFeedback) fi.defaultContent(qtiItem) else fi.content
                if (feedbackContents.foldRight[Boolean](true)((field,acc) => {
                  acc && (feedbackInlines.find(fi => field._1 == fi.csFeedbackId) match {
                    case Some(fi) =>
                      field._2 match {
                      case JsArray(values) => values.contains(JsString(feedbackInlineContent(fi)))
                      case JsString(value) => value == feedbackInlineContent(fi)
                      case _ => false
                    }
                    case None => false
                  })
                })) success
                else failure
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
          case Some((_,jscorrectResponses)) => jscorrectResponses match {
            case JsObject(correctResponses) =>
              if(correctResponses.foldRight[Boolean](true)((prop, acc) => {
                acc && (prop._1 match {
                  case "mexicanPresident" => prop._2.as[String] == "calderon"
                  case "irishPresident" => prop._2.as[String] == "higgins"
                  case "rainbowColors" => prop._2.as[Seq[String]].sameElements(Seq("blue", "violet", "red"))
                  case "winterDiscontent" => prop._2.as[Seq[String]].sameElements(Seq("York", "york"))
                  case "wivesOfHenry" => prop._2.as[Seq[String]].equals(Seq("aragon", "boleyn", "seymour", "cleves", "howard", "parr"))
                  case "cutePugs" => prop._2.as[Seq[String]].equals(Seq("pug1", "pug2", "pug3"))
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

    "only return session data for an item seession if response was already submitted" in {
      /**
       * Low priority for now
       */
      pending
    }

  }

  "item session api" should {


    "return feedback with session" in {
      /*
      val itemId = "50083ba9e4b071cb5ef79101"
      val testSession = ItemSession(new ObjectId(itemId))
      testSession.responses = List(ItemResponse("mexicanPresident", "calderon"))

      val url = "/api/v1/items/" + itemId + "/sessions"
      val request = FakeRequest(
        POST,
        url,
        FakeHeaders(Map("Authorization" -> Seq("Bearer " + token))),
        AnyContentAsJson(Json.toJson(testSession))
      )

      val result = routeAndCall(request)


      if (result.isDefined) {
        result match {
          case Some(result) => {
            val jsonResponse = Json.parse(contentAsString(result))
            (jsonResponse \ "sessionData") match {
              case undefined: JsUndefined => failure
              case sessionData: JsValue =>
                (sessionData \ "feedbackContent") match {
                  case undefined: JsUndefined => failure
                  case feedbackContent: JsValue => {
                    success
                  }
                }
            }
          }
          case None => failure
        }
      }
      else {
        failure
      }
      */
      pending
    }

    "support item creation " in {

      val testSession = ItemSession(new ObjectId(testItemId))
      val url = "/api/v1/items/" + testSession.itemId.toString + "/sessions"

      // add some item responses
      testSession.responses = testSession.responses ++ Seq(ItemResponse("mexicanPresident", "calderon", "{$score:1}"))
      testSession.responses = testSession.responses ++ Seq(ItemResponse("irishPresident", "guinness", "{$score:0}"))
      testSession.responses = testSession.responses ++ Seq(ItemResponse("winterDiscontent", "York", "{$score:1}"))
      testSession.finish = Some(new DateTime())

      val request = FakeRequest(
        POST,
        url,
        FakeHeaders(Map("Authorization" -> Seq("Bearer " + token))),
        AnyContentAsJson(Json.toJson(testSession))
      )

      val optResult = routeAndCall(request)
      if (optResult.isDefined) {
        val json: JsValue = Json.parse(contentAsString(optResult.get))

        // get the generated id
        val id = (json \ "id").asOpt[String].getOrElse("")
        testSession.id = new ObjectId(id)

        // need to implement reader first for below to work
        //val parsedSession = Json.fromJson[ItemSession](json)

        if (doesSessionMatch(json, testSession)) {
          success
        } else {
          failure
        }

      } else {
        // no json back... fail
        failure
      }

    }

    "support retrieval of an itemsession" in {

      val url = "/api/v1/items/" + testSessionIds("itemId") + "/sessions/" + testSessionIds("itemSessionId")
      val getRequest = FakeRequest(
        GET,
        url,
        FakeHeaders(Map("Authorization" -> Seq("Bearer " + token))),
        None
      )

        val optResult = routeAndCall(getRequest)
        if (optResult.isDefined) {
          val json: JsValue = Json.parse(contentAsString(optResult.get))

          // load the test session directly
          val testSessionForGet: ItemSession =
            ItemSession.findOneById(new ObjectId(testSessionIds("itemSessionId"))) match {
              case Some(o) => o
              case None => null
            }

          if (doesSessionMatch(json, testSessionForGet)) {
            success
          } else {
            failure
          }

        } else {
          failure
        }

    }

  }


  def doesSessionMatch(json: JsValue, testSession: ItemSession): Boolean = {
    try {
      val id = (json \ "id").asOpt[String].getOrElse("")
      val itemId = (json \ "itemId").asOpt[String].getOrElse("no id")
      val start = (json \ "start").toString
      val finish = (json \ "finish").toString

      val idString = testSession.id.toString
      val itemIdString = testSession.itemId.toString
      val finishString = testSession.finish.getOrElse(new DateTime(0)).getMillis.toString
      val startString = testSession.start.getMillis.toString

      (finish equals finishString) &&
        (start equals startString) &&
        (id equals idString) &&
        (itemId equals itemIdString)

    } catch {
      case e: Exception => {
        e.printStackTrace()
        false
      }
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
