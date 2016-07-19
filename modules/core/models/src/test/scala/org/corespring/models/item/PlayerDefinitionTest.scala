package org.corespring.models.item

import org.corespring.models.item.resource.StoredFile
import org.specs2.mutable.Specification
import play.api.libs.json.Json._

class PlayerDefinitionTest extends Specification {

  "mergeAllButFiles" should {

    val main = PlayerDefinition(
      Seq(StoredFile("img.png", "image/png", false)),
      "main-xhtml",
      obj(),
      "main-summaryFeedback",
      Some("main-custom-scoring"),
      obj())

    val other = PlayerDefinition(
      Seq.empty,
      "other-xhtml",
      obj("componentType" -> "component-type"),
      "other-summaryFeedback",
      Some("other-custom-scoring"),
      obj("prop" -> "value"))

    val merged = main.mergeAllButFiles(other)

    "not merge files" in { merged.files must_== main.files }
    "merge xhtml" in { merged.xhtml must_== other.xhtml }
    "merge components" in { merged.components must_== other.components }
    "merge summaryFeedback" in { merged.summaryFeedback must_== other.summaryFeedback }
    "merge customScoring" in { merged.customScoring must_== other.customScoring }
    "merge config" in { merged.config must_== other.config }
  }
}
