package tests.web.controllers

import tests.BaseTest
import play.api.test.FakeRequest
import play.api.test.Helpers._
import models.Item
import com.mongodb.{BasicDBObject, DBObject}
import org.bson.types.ObjectId
import play.Routes

class ShowResourceTest extends BaseTest{

  val ItemPlayerRoutes = testplayer.controllers.routes.ItemPlayer

  val TEST_ITEM_ID : String = "50083ba9e4b071cb5ef79101"

  def testItem: Item = item(TEST_ITEM_ID)

  "show resource" should {

    "redirect qti content to the ItemPlayer" in {

      val url = web.controllers.routes.ShowResource.renderDataResource(testItem.id.toString).url
      val request = tokenFakeRequest(GET,  url )

      routeAndCall(request) match {
        case Some(result) => {
          status(result) must equalTo(SEE_OTHER)
          val resultHeaders = headers(result)
          val expectedUrl = ItemPlayerRoutes.previewItem(testItem.id.toString).url
          resultHeaders.get("Location") must equalTo(Some(tokenize(expectedUrl)))
        }
        case _ => failure("request failed")
      }

      true must equalTo(true)
    }
  }

}
