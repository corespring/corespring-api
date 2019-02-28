package org.corespring.container.client.controllers.apps.componentEditor

import org.corespring.container.client.controllers.apps.{ TabComponentEditorOptions, PreviewRightComponentEditorOptions, ComponentEditorOptions }
import org.specs2.mutable.Specification
import org.specs2.specification.Fragments
import play.api.test.FakeRequest

class FormToOptionsTest extends Specification {

  "apply" should {

    "use the default opts if the request has no form" in {
      val request = FakeRequest()
      val options = FormToOptions(request)
      options must_== "tabs" -> ComponentEditorOptions.default
    }

    def assertForm(data: Seq[(String, String)], expected: (String, ComponentEditorOptions)): Fragments = {
      s"parse [${data.map(t => s"${t._1}->${t._2}").mkString(", ")}] to $expected" in {
        val request = FakeRequest().withFormUrlEncodedBody(data: _*)
        val options = FormToOptions(request)
        options must_== expected
      }
    }

    "bothModes" should {

      assertForm(
        Seq("uploadUrl" -> "uploadUrl"),
        "tabs" -> ComponentEditorOptions.default.copy(uploadUrl = Some("uploadUrl")))

      assertForm(
        Seq("uploadMethod" -> "uploadMethod"),
        "tabs" -> ComponentEditorOptions.default.copy(uploadMethod = Some("uploadMethod")))

      assertForm(
        Seq("previewMode" -> "preview-right", "uploadUrl" -> "uploadUrl"),
        "preview-right" -> PreviewRightComponentEditorOptions(None, None, Some("uploadUrl"), None))

      assertForm(
        Seq("previewMode" -> "preview-right", "uploadMethod" -> "uploadMethod"),
        "preview-right" -> PreviewRightComponentEditorOptions(None, None, None, Some("uploadMethod")))

    }

    "with previewMode:preview-right" should {

      val base = Seq("previewMode" -> "preview-right")

      assertForm(
        base ++ Seq("showPreview" -> "true"),
        "preview-right" -> PreviewRightComponentEditorOptions(Some(true), None, None, None))

      assertForm(
        base ++ Seq("previewWidth" -> "300"),
        "preview-right" -> PreviewRightComponentEditorOptions(None, Some(300), None, None))
    }

    "with previewMode:tabs" should {

      val base = Seq("previewMode" -> "tabs")
      assertForm(
        base ++ Seq("activePane" -> "preview"),
        "tabs" -> ComponentEditorOptions.default.copy(activePane = Some("preview")))

      assertForm(
        base ++ Seq("activePane" -> "config"),
        "tabs" -> ComponentEditorOptions.default.copy(activePane = Some("config")))

      assertForm(
        base ++ Seq("activePane" -> "config", "showNavigation" -> "true"),
        "tabs" -> ComponentEditorOptions.default.copy(activePane = Some("config"), showNavigation = Some(true)))

    }

  }
}
