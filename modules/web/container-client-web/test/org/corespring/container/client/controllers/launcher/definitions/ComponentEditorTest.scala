package org.corespring.container.client.controllers.launcher.definitions

import org.corespring.container.client.controllers.launcher.ComponentEditor
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.Json._

class ComponentEditorTest extends Specification with Mockito {

  trait scope extends Scopes.launcher
  "result" should {
    "call builder.buildJs" in new scope {
      val editor = new ComponentEditor(0, builder, Map.empty)
      editor.result("url")
      there was one(builder).buildJs("url", editor.fileNames, editor.options.deepMerge(obj("initTimeout" -> 0)), editor.bootstrap, Map.empty)
    }
  }
}
