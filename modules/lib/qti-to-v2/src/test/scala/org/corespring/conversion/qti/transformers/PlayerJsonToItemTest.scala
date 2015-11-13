package org.corespring.conversion.qti.transformers

import org.bson.types.ObjectId
import org.corespring.models.item.{ PlayerDefinition, Item }
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class PlayerJsonToItemTest extends Specification {

  val item = Item(collectionId = ObjectId.get.toString)

  "wholeItem" should {
    "convert" in pending
  }

  "playerDef" should {
    import PlayerJsonToItem.playerDef
    implicit val pdf = org.corespring.models.json.item.PlayerDefinitionFormat
    val json = Json.obj(
      "xhtml" -> "xhtml",
      "summaryFeedback" -> "summaryFeedback",
      "customScoring" -> "customScoring",
      "components" -> Json.obj("1" -> Json.obj("componenType" -> "blah")))

    "not convert if xhtml is empty" in {
      playerDef(item, json - "xhtml") must_== item
    }

    "convert the json" in {
      playerDef(item, json).playerDefinition.get must_== json.as[PlayerDefinition]
    }
  }

  "profile" should {
    "convert" in pending
  }

  "standards" should {
    "convert" in pending
  }

  "taskInfo" should {
    "convert" in pending
  }

  "subjects" should {
    "convert" in pending
  }

  "subjects" should {
    "convert" in pending
  }

  "contributorDetails" should {
    "convert" in pending
  }

  "copyright" should {
    "convert" in pending
  }

  "additionalCopyrights" should {
    "convert" in pending
  }

  "workflow" should {
    "convert" in pending
  }

  "supportingMaterials" should {
    "convert" in pending
  }
}
