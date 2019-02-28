package org.corespring.container.client.controllers.resources.session

import org.corespring.container.components.processing.ItemPruner
import org.corespring.test.utils.JsonCompare
import org.specs2.mutable.Specification
import play.api.libs.json.Json
import org.corespring.container.logging.ContainerLogger

class ItemPrunerTest extends Specification with ItemPruner {

  "item pruner" should {
    "prune" in {

      val jsonString =
        """
          |{
          |  "xhtml" : "hello",
          |  "components" : {
          |    "2" : {
          |      "feedback" : { "value" : "2" },
          |      "correctResponse" : { "value" : "1" },
          |      "model" : { "value" : "1" }
          |    }
          |  }
          |}
        """.stripMargin

      val expected =
        """
          |{
          |  "xhtml" : "hello",
          |  "components" : {
          |    "2" : {
          |      "model" : { "value" : "1" }
          |    }
          |  }
          |}
        """.stripMargin

      val json = Json.parse(jsonString)

      val result = pruneItem(json)

      println(Json.stringify(result))

      JsonCompare.caseInsensitiveSubTree(expected, Json.stringify(result)) match {
        case Right(_) => success
        case Left(diffs) => {
          println(diffs.mkString("\n"))
          failure
        }
      }
    }
  }

  def logger = ContainerLogger.getLogger("Test")
}
