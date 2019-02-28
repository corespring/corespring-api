package org.corespring.container.client.pages

import org.corespring.container.client.V2PlayerConfig
import org.corespring.container.client.component.ComponentsScriptBundle
import org.corespring.container.client.controllers.apps.{ ItemEditorEndpoints, StaticPaths }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.client.views.txt.js.CatalogServices
import org.corespring.container.components.model.Component
import org.corespring.container.components.services.ComponentService
import org.mockito.Matchers.{ eq => m_eq }
import org.specs2.execute.Result
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.{ Fragments, Scope }
import org.specs2.time.NoTimeConversions
import play.api.libs.json.{ JsArray, Json }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

class CatalogRendererTest extends Specification with Mockito with NoTimeConversions {

  trait scope extends Scope {

    lazy val playerConfig = V2PlayerConfig(None, None)
    lazy val jadeEngine = RendererMocks.jadeEngine
    lazy val pageSourceService = RendererMocks.pageSourceService
    lazy val componentJson = RendererMocks.componentJson
    lazy val assetPathProcessor = RendererMocks.assetPathProcessor
    lazy val containerContext = ContainerExecutionContext.TEST
    lazy val componentService = new ComponentService {
      override def components: Seq[Component] = Nil
    }

    lazy val renderer = new CatalogRenderer(playerConfig, jadeEngine, containerContext, pageSourceService, componentJson, componentService, assetPathProcessor)

    protected def waitFor[A](f: Future[A]): A = Await.result(f, 1.second)
  }

  trait render extends scope {
    lazy val bundle = ComponentsScriptBundle(Nil, Seq("comp.js"), Seq("comp.css"), Seq("comp.ng.module"))
    lazy val mainEndpoints = ItemEditorEndpoints.main("itemId")
    lazy val supportingMaterialsEndpoints = ItemEditorEndpoints.supportingMaterials("itemId")
    lazy val prodMode = false

    lazy val html = renderer.render(bundle, mainEndpoints, supportingMaterialsEndpoints, Map("queryParamOne" -> "1"), prodMode, "check", Json.obj())
    waitFor(html)

    lazy val captor = {
      capture[Map[String, Any]]
    }
  }

  def assertJadeParam(prodMode: Boolean = false)(key: String, assertFn: Option[Any] => Result): Fragments = {
    s"it passes in $key ${if (prodMode) "- prod" else ""}" in new render {
      there was one(jadeEngine).renderJade(m_eq("catalog"), captor)
      assertFn(captor.value.get(key))
    }
  }

  val devModeAssert = assertJadeParam(false) _

  "render" should {

    devModeAssert("appName", v => v === Some("catalog"))
    devModeAssert("js", v => v.map(_.asInstanceOf[Array[Any]].toSeq) === Some(Seq("comp.js")))
    devModeAssert("css", v => v.map(_.asInstanceOf[Array[Any]].toSeq) === Some(Seq("comp.css")))
    devModeAssert("ngModules", v => v === Some("'catalog-injected','comp.ng.module'"))
    devModeAssert("staticPaths", v => v === Some(Json.stringify(StaticPaths.staticPaths)))
    devModeAssert("ngServiceLogic", v => {
      val componentSet = JsArray(Seq.empty)
      require(Json.stringify(componentSet) == "[]")
      val queryParamsJson = Json.obj("queryParamOne" -> "1")
      val ngServiceLogic = CatalogServices(
        "catalog-injected",
        componentSet,
        ItemEditorEndpoints.main("itemId"),
        ItemEditorEndpoints.supportingMaterials("itemId"),
        queryParamsJson).toString
      v === Some(ngServiceLogic)
    })

    "return html" in new render {
      html.map(_.toString) must equalTo("<html></html>").await
    }
  }
}
