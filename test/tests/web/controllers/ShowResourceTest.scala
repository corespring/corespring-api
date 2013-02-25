package tests.web.controllers

import tests.BaseTest
import play.api.test.FakeRequest
import play.api.test.Helpers._
import com.mongodb.{BasicDBObject, DBObject}
import org.bson.types.ObjectId
import play.Routes
import models.item.Item

class ShowResourceTest extends BaseTest{

  val ItemPlayerRoutes = testplayer.controllers.routes.ItemPlayer

  val TEST_ITEM_ID : String = "511156d38604c9f77da9739d"

  def testItem: Item = item(TEST_ITEM_ID)

  "show resource" should {

    "show qti content in the ItemPlayer" in {

      val url = web.controllers.routes.ShowResource.renderDataResource(testItem.id.toString).url
      val request = tokenFakeRequest(GET,  url )

      routeAndCall(request) match {
        case Some(result) => status(result) must equalTo(OK)
        case _ => failure("request failed")
      }
    }
  }

}
