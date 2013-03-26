package tests.models.itemSession

import play.api.libs.json.{Json}
import tests.{BaseTest}
import models.itemSession.{ItemSessionSettings, ItemSession}

class ItemSessionSettingsTest extends BaseTest{

  "item session settings " should {
    "parse json" in {

      val settings = ItemSessionSettings()

      val json = Json.toJson(settings)

      (json \ "maxNoOfAttempts").as[Int] must equalTo(1)
      (json \ "highlightUserResponse").as[Boolean] must equalTo(true)
      (json \ "highlightCorrectResponse").as[Boolean] must equalTo(true)
      (json \ "showFeedback").as[Boolean] must equalTo(true)
      (json \ "allowEmptyResponses").as[Boolean] must equalTo(false)
      (json \ "submitCompleteMessage").as[String] must equalTo(ItemSessionSettings.SubmitComplete)
      (json \ "submitIncorrectMessage").as[String] must equalTo(ItemSessionSettings.SubmitIncorrect)

    }

    "parse settings in ItemSession" in {

      val s =
        """{
           "id":"508aafec3004ad38b5ac1819",
           "itemId":"50083ba9e4b071cb5ef79101",
           "responses":[
             {
               "id":"mexicanPresident",
               "value":"calderon",
               "outcome":"{$score:1}"},
             {
               "id":"irishPresident",
               "value":"guinness",
               "outcome":"{$score:0}"},
             {
               "id":"winterDiscontent",
               "value":"York",
               "outcome":"{$score:1}"}
             ],
           "finish":1351266284812
           }"""
      val json = Json.parse(s)

      val session = json.as[ItemSession]

      session.settings must equalTo(new ItemSessionSettings())
    }
  }

}
