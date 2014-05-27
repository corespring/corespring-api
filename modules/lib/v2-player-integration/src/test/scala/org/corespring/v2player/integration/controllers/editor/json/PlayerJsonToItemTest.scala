package org.corespring.v2player.integration.controllers.editor.json

import org.specs2.mutable.Specification
import play.api.libs.json.Json
import org.corespring.platform.core.models.item.Item
import org.bson.types.ObjectId

class PlayerJsonToItemTest extends Specification {

  "PlayerJson => Item" should {

    "work for profile " in {

      val jsonString = """
            {
                "taskInfo": {
                "title": "Drag and drop example",
                        "gradeLevel": [
                "01",
                        "03"
                ],
                "primarySubject": {
                    "id": "4ffb535f6bb41e469c0bf2a8",
                            "text": "Performing Arts"
                },
                "relatedSubject": {
                    "id": "4ffb535f6bb41e469c0bf2a9",
                            "text": "AP Music Theory,Visual Arts"
                },
                "itemType": "Type"
              }
            }"""

      val json = Json.parse(jsonString)
      val item = Item()
      val update = PlayerJsonToItem.profile(item, json)

      update.taskInfo.map { info =>
        info.title === Some("Drag and drop example")
        info.itemType === Some("Type")
        info.gradeLevel === Seq("01", "03")
        info.subjects.get.primary === Some(new ObjectId("4ffb535f6bb41e469c0bf2a8"))
        info.subjects.get.related === Some(new ObjectId("4ffb535f6bb41e469c0bf2a9"))
      }.getOrElse(failure("No updated info"))
    }

    "work for xhtml + components" in {

      val jsonString =
        """
          |{
          |  "xhtml" : "<div/>",
          |  "components" : {
          |    "1" : {
          |      "componentType" : "test-component"
          |    }
          |  },
          |  "summaryFeedback" : "some feedback"
          |}
        """.stripMargin

      val json = Json.parse(jsonString)
      val item = Item()
      val update = PlayerJsonToItem.playerDef(item, json)

      update.playerDefinition.get.xhtml === "<div/>"
      (update.playerDefinition.get.components \ "1" \ "componentType").as[String] === "test-component"
      update.playerDefinition.get.summaryFeedback === "some feedback"

    }
  }

}
