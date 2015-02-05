package org.corespring.platform.core.models.item

import org.specs2.mutable.Specification
import play.api.libs.json.Json

class PlayerDefinitionTest extends Specification {

  "itemTypes" should {

    val itemTypes = Map("corespring-multiple-choice" -> 1, "corespring-text-entry" -> 2)
    val components = itemTypes.map{ case(itemType, count) => List.fill(count)(Json.obj("componentType" -> itemType)) }
      .flatten.zipWithIndex.map{ case (obj, index) => Json.obj(index.toString -> obj) }
      .foldLeft(Json.obj()){ (obj, acc) => acc ++ obj }
    val playerDefinition = new PlayerDefinition(files = Seq.empty, xhtml = "",
      components = components, summaryFeedback = "", customScoring = None)

    "return counts of item types present in components" in {
      playerDefinition.itemTypes must beEqualTo(itemTypes)
    }

  }

}
