package org.corespring.container.client.pages

import org.corespring.container.client.VersionInfo
import org.corespring.container.client.component.ComponentsScriptBundle
import org.corespring.container.client.controllers.apps.{ EditorClientOptions, ItemEditorEndpoints, StaticPaths }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.client.views.models.ComponentsAndWidgets
import org.corespring.container.client.views.txt.js.EditorServices
import org.mockito.Matchers.{ eq => m_eq }
import org.specs2.execute.Result
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.{ Fragments, Scope }
import org.specs2.time.NoTimeConversions
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test.{ DefaultAwaitTimeout, Helpers }

class EditorRendererTest extends Specification with Mockito with NoTimeConversions with DefaultAwaitTimeout {

  trait scope extends Scope {

    lazy val renderer = new EditorRenderer {
      override lazy val jade = RendererMocks.jadeEngine
      override lazy val pageSourceService = RendererMocks.pageSourceService
      override lazy val componentJson = RendererMocks.componentJson
      override lazy val assetPathProcessor = RendererMocks.assetPathProcessor
      override lazy val containerExecutionContext: ContainerExecutionContext = ContainerExecutionContext.TEST
      override lazy val versionInfo: VersionInfo = VersionInfo("version", "commitHash", "branch", "date", Json.obj())
      override lazy val name: String = "editor"
    }

  }

  trait render extends scope {
    lazy val mainEndpoints = ItemEditorEndpoints.main("itemId")
    lazy val supportingMaterialEndpoints = ItemEditorEndpoints.supportingMaterials("itemId")
    lazy val componentsAndWidgets = ComponentsAndWidgets(Json.obj(), Json.obj())
    lazy val clientOptions = EditorClientOptions(0, 8 * 1024, 500, StaticPaths.staticPaths)
    lazy val bundle = ComponentsScriptBundle(Nil, Seq("comp.js"), Seq("comp.css"), Seq("comp.ng.module"))
    def prodMode = true
    lazy val html = renderer.render(mainEndpoints, supportingMaterialEndpoints, componentsAndWidgets, clientOptions, bundle, Map("apple" -> "apple"), prodMode, "check", Json.obj())
    Helpers.await(html)

    lazy val captor = {
      capture[Map[String, Any]]
    }
  }

  def assertJadeParam(pm: Boolean = false)(key: String, assertFn: Option[Any] => Result): Fragments = {
    s"pass in $key ${if (pm) "- prod" else ""}to jadeEngine.renderJade" in new render {
      override lazy val prodMode = pm
      there was one(renderer.jade).renderJade(m_eq("editor"), captor)
      assertFn(captor.value.get(key))
    }
  }

  val devModeAssert = assertJadeParam(false) _

  "render" should {

    devModeAssert("appName", v => v === Some("editor"))
    devModeAssert("js", v => v.map(_.asInstanceOf[Array[Any]].toSeq) === Some(Seq("comp.js")))
    devModeAssert("css", v => v.map(_.asInstanceOf[Array[Any]].toSeq) === Some(Seq("comp.css")))
    devModeAssert("ngModules", v => v === Some("'editor.serverInjectedServices','comp.ng.module'"))

    devModeAssert("ngServiceLogic", v => {
      val queryParamsJson = Json.obj("apple" -> "apple")
      val ngServiceLogic = EditorServices(
        "editor.serverInjectedServices",
        ItemEditorEndpoints.main("itemId"),
        ItemEditorEndpoints.supportingMaterials("itemId"),
        ComponentsAndWidgets(Json.obj(), Json.obj()),
        queryParamsJson).toString
      v === Some(ngServiceLogic)
    })

    "return html" in new render {
      html.map(_.toString) must equalTo("<html></html>").await
    }
  }

}
