package tests.web.controllers.testplayer.qti

import tests.BaseTest
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import play.api.mvc.AnyContentAsJson
import play.api.libs.json.Json
import xml.{NodeSeq, XML}

class ItemPlayerTest extends BaseTest {

  "add outcomeidentifier and identifier to feedback elements defined within choices" in {
    val itemId = "505d839b763ebc84ac34d484"

    /**
     *  @see https://trello.com/card/add-outcomeidentifier-and-identifier-to-feedback-elements-defined-within-choices/500f0e4cf207c721072011c1/90
     *
     *  For now This should happen when the xml is being sent to the test player
     */
    val fakeGet = FakeRequest(GET, "/testplayer/item/%s?access_token=%s".format(itemId, token))
    val getResult = routeAndCall(fakeGet).get
    status(getResult) must equalTo(OK)

    // XML parser can't understand the doctype, so parse XML starting at <html> tab
    val content = contentAsString(getResult)
    val xmlResponse = XML.loadString(content.substring(content.indexOf("<html")))

    println(xmlResponse)

    (xmlResponse \ "body" \\ "feedbackInline").foreach(feedbackInline => {
      (feedbackInline \ "@outcomeIdentifier").text must not beEmpty
    })
  }

}
