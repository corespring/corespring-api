package tests.web.controllers.testplayer.qti

import tests.BaseTest
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import play.api.mvc.AnyContentAsJson
import play.api.libs.json.Json
import xml.{NodeSeq, XML}

class ItemPlayerTest extends BaseTest {

  val itemWithNoIdentifiersId = "505d839b763ebc84ac34d484"
  val itemWithSomeIdentifiersId = "505e704c03e1112792e383ab"

  "add outcomeidentifier and identifier to feedback elements defined within choices" in {
    getFeedbackFromPlayer(itemWithNoIdentifiersId).foreach(feedbackInline => {
      val identifier = (feedbackInline \ "@identifier").text
      val outcomeIdentifier = (feedbackInline \ "@outcomeIdentifier").text

      outcomeIdentifier must not beEmpty;
      outcomeIdentifier must beMatching(".*responses.*.value")
      identifier must not beEmpty
    })
  }

  "do not add outcomeIdentifier or identifier to feedback elements if already present" in {
    getFeedbackFromPlayer(itemWithSomeIdentifiersId).foreach(feedback => {
      val outcomeIdentifier = (feedback \ "@outcomeIdentifier").text
      val identifier = (feedback \ "@identifier").text
      (feedback \ "@csFeedbackId").text match {
        case "feedbackChoiceA" => {
          outcomeIdentifier must beEqualTo("RESPONSE")
          identifier must beEqualTo("ChoiceA")
        }
        case "feedbackChoiceB" => {
          outcomeIdentifier must beEqualTo("RESPONSE")
          identifier must beEqualTo("ChoiceD")
        }
        case "feedbackChoiceC" => {
          outcomeIdentifier must beMatching(".*responses.*.value")
          identifier must beEqualTo("ChoiceD")
        }
      }
    })
  }

  private def getFeedbackFromPlayer(itemId: String): NodeSeq = {
    val fakeGet = FakeRequest(GET, "/testplayer/item/%s?access_token=%s".format(itemId, token))
    val getResult = routeAndCall(fakeGet).get
    status(getResult) must equalTo(OK)

    // XML parser can't understand the doctype, so parse XML starting at <html> tab
    val content = contentAsString(getResult)
    XML.loadString(content.substring(content.indexOf("<html"))) \ "body" \\ "feedbackInline"
  }

}
