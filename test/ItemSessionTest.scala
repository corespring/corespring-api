import models.{Item, ItemResponse, ItemSession}
import org.bson.types.ObjectId
import org.specs2.execute.Pending
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsJson
import play.api.test.{FakeHeaders, FakeRequest}
import org.specs2.mutable._
import play.api.test.Helpers._

/**
 * Tests the ItemSession model
 */
class ItemSessionTest extends Specification {

  PlaySingleton.start()

  // from standard fixture data
  val token = "34dj45a769j4e1c0h4wb"
  val testItemId = "5001b7ade4b0d7c9ec321070"
  val testItemSessionId = "502d0f823004deb7f4f53be7"

  // create test session bound to random object id
  // in practice ItemSessions need to be bound to an item
  val testSession = ItemSession(Some(new ObjectId()),Some(new ObjectId()))

  "ItemSession" should {
    "be saveable" in {
      ItemSession.save(testSession)
      ItemSession.findOneById(testSession.id.get) match {
        case Some(result) => success
        case _ => failure
      }
    }

    "be deletable" in {
      ItemSession.remove(testSession)
      ItemSession.findOneById(testSession.id.get) match {
        case Some(result) => failure
        case _ => success
      }
    }
  }





  "item session api" should {
    "support item creation " in {



      val testSession = ItemSession(None, Some(new ObjectId(testItemId)))
      val url = "/api/v1/items/" + testSession.itemId.get.toString + "/sessions"

      // add some item responses
      testSession.responses ::= ItemResponse("question1","choice1", "{$score:1}")
      testSession.responses ::= ItemResponse("question2","some text", "{$score:1}")
      testSession.responses ::= ItemResponse("question3","more text", "{$score:1}")

      val request = FakeRequest(
        POST,
        url,
        FakeHeaders( Map("Authorization" -> Seq("Bearer "+token)) ),
        AnyContentAsJson(Json.toJson(testSession))
      )

      System.out.println(Json.toJson(testSession))

      val optResult = routeAndCall(request)
      if(optResult.isDefined) {


        val json:JsValue = Json.parse(contentAsString(optResult.get))

        // get the generated id
        val id = (json \ "id").asOpt[String].getOrElse("")
        testSession.id = Option(new ObjectId(id))

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
  }

  "item session api" should {
    "support retrieval of an itemsession" in {

      val url = "/api/v1/itemsessions/" + testItemSessionId
      val getRequest = FakeRequest(
        GET,
        url,
        FakeHeaders( Map("Authorization" -> Seq("Bearer "+token)) ),
        None
      )

      try {
        val optResult = routeAndCall(getRequest)
        if (optResult.isDefined) {
          val json:JsValue = Json.parse(contentAsString(optResult.get))

          // load the test session directly
          val testSessionForGet : ItemSession = ItemSession.findOneById(new ObjectId(testItemSessionId)) match {
            case Some(o) => o
            case None =>  null
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
          System.out.println(e.getMessage)
          failure
        }
      }
    }
  }


  def doesSessionMatch(json:JsValue, testSession:ItemSession) : Boolean = {
    try {
      val id = (json \ "id").asOpt[String].getOrElse("")
      val itemId = (json \ "contentId").asOpt[String].getOrElse("")
      val start = (json \ "start").asOpt[String]
      val finish = (json \ "finish").asOpt[String]

      val idString = testSession.id.get.toString
      val itemIdString = testSession.itemId.get.toString
      val finishString = testSession.finish.getMillis.toString
      val startString = testSession.start.getMillis.toString

      var result = false
      result = finish.get == finishString
      result = start.get == startString
      result = id == idString
      result = itemId == itemIdString

      result
      } catch {
      case e: Exception => false
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
