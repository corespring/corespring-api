package tests.web.controllers.testplayer.qti

import tests.BaseTest
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import play.api.mvc.AnyContentAsJson
import play.api.libs.json.{JsValue, Json}
import xml.{NodeSeq, XML}

class ItemPlayerTest extends BaseTest {

  val itemWithNoIdentifiersId = "505d839b763ebc84ac34d484"
  val itemWithSomeIdentifiersId = "505e704c03e1112792e383ab"

  val itemWithFeedbackId = "505d839b763ebc84ac34d484"

  "works with test player example item" in {
    val fakeGet = FakeRequest(GET, "/testplayer/item/%s?access_token=%s".format( "50083ba9e4b071cb5ef79101", token))
    val getResult = routeAndCall(fakeGet).get
    status(getResult) must equalTo(OK)
  }

  "add outcomeidentifier and identifier to feedback elements defined within choices" in {
    pending
    getFeedbackFromItem(itemWithNoIdentifiersId).foreach(feedbackInline => {
      val identifier = (feedbackInline \ "@identifier").text
      val outcomeIdentifier = (feedbackInline \ "@outcomeIdentifier").text

      outcomeIdentifier must not beEmpty;
      outcomeIdentifier must beMatching(".*responses.*.value")
      identifier must not beEmpty
    })
  }

  "do not add outcomeIdentifier or identifier to feedback elements if already present" in {
    pending
    getFeedbackFromItem(itemWithSomeIdentifiersId).foreach(feedback => {
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

  "getting feedback for invalid responseIdentifier returns 404" in {
    val fakeGet = FakeRequest(GET,
      "/testplayer/item/%s/feedback/%s/%s?access_token=%s".format(
        itemWithNoIdentifiersId, "invalid_response_id", "invalid_id", token))

    val getResult = routeAndCall(fakeGet).get
    status(getResult) must equalTo(NOT_FOUND)
  }

  "get feedback from identifiers" in {
    getFeedback(itemWithFeedbackId, "RESPONSE", "ChoiceA").foreach(feedback => {

      feedback must haveKey("csFeedbackId")
      feedback must haveKey("body")

      val csFeedbackId = feedback.get("csFeedbackId").get
      val body = feedback.get("body").get

      csFeedbackId must be equalTo "feedbackChoiceA"
      body must be containing "<p>This is the correct answer!</p>"
    })

    getFeedback(itemWithFeedbackId, "RESPONSE", "ChoiceB").foreach(feedback => {
      feedback must haveKey("csFeedbackId")
      feedback must haveKey("body")

      val csFeedbackId = feedback.get("csFeedbackId").get
      val body = feedback.get("body").get

      csFeedbackId must be equalTo "feedbackChoiceB"
      body must be containing "<p>This is the incorrect answer!</p>"
    })

    getFeedback(itemWithFeedbackId, "RESPONSE", "ChoiceC").foreach(feedback => {
      feedback must haveKey("csFeedbackId")
      feedback must haveKey("body")

      val csFeedbackId = feedback.get("csFeedbackId").get
      val body = feedback.get("body").get

      csFeedbackId must be equalTo "feedbackChoiceC"
      body must be containing "<p>This is the incorrect answer!</p>"
    })
  }

  private def getFeedbackFromItem(itemId: String): NodeSeq = {
    val fakeGet = FakeRequest(GET, "/testplayer/item/%s?access_token=%s".format(itemId, token))
    val getResult = routeAndCall(fakeGet).get
    status(getResult) must equalTo(OK)

    // XML parser can't understand the doctype, so parse XML starting at <html> tab
    val content = contentAsString(getResult)
    XML.loadString(content.substring(content.indexOf("<html"))) \ "body" \\ "feedbackInline"
  }


  private def getFeedback(itemId: String, responseIdentifier: String, identifier: String): Set[Map[String, String]] = {
    val fakeGet = FakeRequest(GET,
      "/testplayer/item/%s/feedback/%s/%s?access_token=%s".format(
        itemId, responseIdentifier, identifier, token))

    val getResult = routeAndCall(fakeGet).get
    status(getResult) must equalTo(OK)

    (Json.parse(contentAsString(getResult)) \ "feedback")
      .asOpt[Set[JsValue]].getOrElse(Set())
      .map(_.asOpt[Map[String, String]].getOrElse(Map()))
  }

}
