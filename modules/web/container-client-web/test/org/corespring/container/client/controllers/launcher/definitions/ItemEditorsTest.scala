package org.corespring.container.client.controllers.launcher.definitions

import org.corespring.container.client.controllers.launcher.ItemEditors
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.Json._

class ItemEditorsTest extends Specification with Mockito {

  trait scope extends Scopes.launcher {
    val editors = new ItemEditors(10, builder, Map.empty)
  }

  "result" should {

    "call builder.buildJs" in new scope {
      val result = editors.result("url")
      there was one(builder).buildJs("url", editors.fileNames, editors.options.deepMerge(obj("initTimeout" -> 10)), editors.bootstrap, Map.empty)
    }

  }
}
