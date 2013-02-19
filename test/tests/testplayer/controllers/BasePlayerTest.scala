package tests.testplayer.controllers

import org.specs2.mutable.Specification
import testplayer.controllers.BasePlayer
import play.api.mvc._
import models.ItemSession
import play.api.test.FakeRequest
import com.mongodb.casbah.commons.MongoDBObject
import tests.{BaseTest, PlaySingleton}
import play.api.test.Helpers._
import play.api.mvc.SimpleResult
import play.api.libs.iteratee.Enumerator
import org.bson.types.ObjectId

class BasePlayerTest extends BaseTest {

  "Base player" should {

    "render correctly" in {

      val player = new BasePlayer{
        def OkRunPlayer(xml:String,itemId:String,sessionId:String,token:String) = {
          SimpleResult[String](ResponseHeader(200), Enumerator("hooray"))
        }
      }

      val sessionId = new ObjectId("51116bc7a14f7b657a083c1d")
      val request = FakeRequest("GET","blah?access_token=" + token )
      val result : play.api.mvc.Result  = player.runBySessionId(sessionId)(request)
      contentAsString(result) === "hooray"
    }
  }

}
