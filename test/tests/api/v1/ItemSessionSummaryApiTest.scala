package tests.api.v1

import utils.RequestCalling
import org.specs2.mutable.Specification
import play.api.test.FakeRequest
import play.api.mvc.AnyContentAsJson
import play.api.libs.json.{JsObject, Json}
import models.itemSession.{ItemSessionSummary, ItemSession}
import play.api.test.Helpers._
import tests.PlaySingleton

class ItemSessionSummaryApiTest extends Specification with RequestCalling {

  PlaySingleton.start()

  "ItemSessionApi" should {
    "work" in {

      val call = api.v1.routes.ItemSessionSummaryApi.multiple()

      val idOne = "51116c6287eb055332a2f8e4"

      val ids = Map("ids" -> List(idOne))

      val request = FakeRequest(
        call.method,
        call.url,
        FakeAuthHeader,
        AnyContentAsJson(Json.toJson(ids)))

      routeAndCall(request) match {
        case Some(result) => {
          val jsonString = contentAsString(result)
          println(jsonString)
          val json = Json.parse(jsonString)
          json.asOpt[List[JsObject]] match {
            case Some(list) => {
              list.length === 1
              (list(0) \ "sessionId").as[String] === idOne
              success
            }
            case _ => failure
          }
        }
        case _ => failure
      }
    }
  }

}
