import models.{ItemResponse, ItemSession}
import org.bson.types.ObjectId
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


  // create test session bound to random object id
  // in practice ItemSessions need to be bound to an item
  val testSession = ItemSession(Some(new ObjectId()),new ObjectId())

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

      // from standard fixture data
      val token = "34dj45a769j4e1c0h4wb"
      // TODO this is currently passing a new itemId - this should fail. itemId needs to exist in items collection
      val testSession = ItemSession(None, new ObjectId())
      val url = "/api/v1/items/" + testSession.itemId.toString + "/sessions"

      // add some item responses
      testSession.responses ::= ItemResponse("question1","choice1", "outcome:{$score:1}")
      testSession.responses ::= ItemResponse("question2","some text", "outcome:{$score:1}")
      testSession.responses ::= ItemResponse("question3","more text", "outcome:{$score:1}")

      val request = FakeRequest(
        POST,
        url,
        FakeHeaders( Map("Authorization" -> Seq("Bearer "+token)) ),
        AnyContentAsJson(Json.toJson(testSession))
      )


      val optResult = routeAndCall(request)
      if(optResult.isDefined) {


        val json:JsValue = Json.parse(contentAsString(optResult.get))
        // need to implement reader first for below to work
        //val parsedSession = Json.fromJson[ItemSession](json)

        val id = (json \ "id").asOpt[String].getOrElse("")
        val itemId = (json \ "itemId").asOpt[String].getOrElse("")
        val start = (json \ "start").asOpt[String]
        val finish = (json \ "finish").asOpt[String]


        val finishString = testSession.finish.getMillis.toString
        val startString = testSession.start.getMillis.toString
        if (finish.get == finishString && start.get == startString) {

          success
        }  else {
          failure
        }


      } else {
        // no json back... fail
        failure
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
