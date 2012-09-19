package tests.api.v1

import models.{ItemResponse, ItemSession, Item}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.specs2.execute.Pending
import play.api.libs.json.{JsUndefined, JsValue, Json}
import play.api.mvc.AnyContentAsJson
import play.api.test.{FakeHeaders, FakeRequest}
import org.specs2.mutable._
import play.api.test.Helpers._
import scala.Some
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeHeaders
import scala.Some
import tests.PlaySingleton

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


  "item session api" should {

    "return feedback with session" in {
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

      println(testSession.itemId.toString)

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

      try {
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

      } catch {
        case e: Exception => {
          e.printStackTrace
          failure
        }
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
