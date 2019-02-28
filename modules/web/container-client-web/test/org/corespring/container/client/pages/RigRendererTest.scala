package org.corespring.container.client.pages

import org.corespring.container.client.component.SingleComponentScriptBundle
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.components.model.Component
import org.corespring.container.components.model.dependencies.ComponentMaker
import org.corespring.container.components.services.ComponentService
import org.mockito.Matchers.{ eq => m_eq }
import org.specs2.execute.Result
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.{ Fragments, Scope }
import org.specs2.time.NoTimeConversions
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

class RigRendererTest extends Specification with NoTimeConversions with Mockito with ComponentMaker {

  trait scope extends Scope {

    lazy val jadeEngine = RendererMocks.jadeEngine
    lazy val pageSourceService = RendererMocks.pageSourceService
    lazy val componentJson = RendererMocks.componentJson
    lazy val assetPathProcessor = RendererMocks.assetPathProcessor
    lazy val containerContext = ContainerExecutionContext.TEST
    lazy val componentService = new ComponentService {
      override def components: Seq[Component] = Nil
    }

    lazy val renderer = new RigRenderer(pageSourceService, containerContext, jadeEngine)

    protected def waitFor[A](f: Future[A]): A = Await.result(f, 1.second)
  }

  trait render extends scope {
    lazy val bundle = SingleComponentScriptBundle(uiComp("one", Nil), Seq("comp.js"), Seq("comp.css"), Seq("comp.ng.module"))
    lazy val html = renderer.render(Json.obj(), bundle)
    waitFor(html)

    lazy val captor = {
      capture[Map[String, Any]]
    }
  }

  def assertJadeParam(prodMode: Boolean = false)(key: String, assertFn: Option[Any] => Result): Fragments = {
    s"pass in $key ${if (prodMode) "- prod" else ""}to jadeEngine.renderJade" in new render {
      there was one(jadeEngine).renderJade(m_eq("rig"), captor)
      assertFn(captor.value.get(key))
    }
  }

  val devModeAssert = assertJadeParam(false) _

  "render" should {

    devModeAssert("appName", v => v === Some("rig"))
    devModeAssert("js", v => v.map(_.asInstanceOf[Array[Any]].toSeq) === Some(Seq("comp.js")))
    devModeAssert("css", v => v.map(_.asInstanceOf[Array[Any]].toSeq) === Some(Seq("comp.css")))
    devModeAssert("ngModules", v => v === Some("'comp.ng.module'"))

    "return html" in new render {
      html.map(_.toString) must equalTo("<html></html>").await
    }
  }
}
