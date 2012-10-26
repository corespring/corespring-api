package tests.testplayer.controllers

import tests.BaseTest
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.libs.json.{JsValue, Json}
import xml.{NodeSeq, XML}

class ItemPlayerTest extends BaseTest {

  val ItemPlayerRoutes = testplayer.controllers.routes.ItemPlayer

  val itemWithNoIdentifiersId = "505d839b763ebc84ac34d484"
  val itemWithSomeIdentifiersId = "505e704c03e1112792e383ab"

  val itemWithFeedbackId = "505d839b763ebc84ac34d484"

  "works with test player example item" in {
    val call = ItemPlayerRoutes.previewItem("50083ba9e4b071cb5ef79101")
    val fakeGet = FakeRequest(call.method, (call.url+"?access_token=%s").format(token))
    val getResult = routeAndCall(fakeGet).get
    status(getResult) must equalTo(OK)
  }

}
