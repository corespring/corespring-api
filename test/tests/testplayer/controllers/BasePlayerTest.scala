package tests.testplayer.controllers

import org.specs2.mutable.Specification
import testplayer.controllers.BasePlayer
import play.api.mvc._
import models.ItemSession
import play.api.test.FakeRequest
import com.mongodb.casbah.commons.MongoDBObject
import tests.PlaySingleton
import play.api.test.Helpers._
import play.api.mvc.SimpleResult
import play.api.libs.iteratee.Enumerator

class BasePlayerTest extends Specification {

  PlaySingleton.start()

  "Base player" should {

    "render correctly" in {

      val player = new BasePlayer{
        def OkRunPlayer(xml:String,itemId:String,sessionId:String,token:String) = {
          SimpleResult[String](ResponseHeader(200), Enumerator("hooray"))
        }
      }

      val sessionId = ItemSession.findOne(MongoDBObject()).get.id
      val request = FakeRequest("GET","blah?access_token=" + common.mock.MockToken )
      val result : play.api.mvc.Result  = player.runBySessionId(sessionId)(request)
      contentAsString(result) === "hooray"
    }
  }

}
