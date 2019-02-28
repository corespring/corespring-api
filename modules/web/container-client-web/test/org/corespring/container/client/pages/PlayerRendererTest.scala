package org.corespring.container.client.pages

import org.corespring.container.client.component.ComponentsScriptBundle
import org.corespring.container.client.controllers.apps.{ NgSourcePaths, PlayerEndpoints }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.client.views.txt.js.PlayerServices
import org.corespring.container.client.{ V2PlayerConfig, VersionInfo }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.json.Json._

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

class PlayerRendererTest extends Specification with Mockito with NoTimeConversions {

  trait scope extends Scope {
    lazy val prodMode = false
    lazy val showControls = false
    lazy val bundle = ComponentsScriptBundle(Nil, Nil, Nil, Nil)

    lazy val playerConfig = V2PlayerConfig(None, None)
    lazy val containerContext = ContainerExecutionContext.TEST
    lazy val jadeEngine = RendererMocks.jadeEngine
    lazy val pageSourceService = RendererMocks.pageSourceService

    pageSourceService.loadJs("player-controls") returns NgSourcePaths(Seq("controls.js"), "controls-prod.js", Nil, Seq("ng.controls"))

    lazy val assetPathProcessor = RendererMocks.assetPathProcessor
    lazy val componentJson = RendererMocks.componentJson
    lazy val playerXhtml = RendererMocks.playerXhtml
    lazy val itemPreProcessor = RendererMocks.itemPreProcessor
    lazy val versionInfo = VersionInfo("", "", "", "", obj())

    def waitFor[A](f: Future[A]): A = Await.result(f, 1.second)

    lazy val renderer = new PlayerRenderer(
      playerConfig,
      containerContext,
      jadeEngine,
      pageSourceService,
      assetPathProcessor,
      componentJson,
      playerXhtml,
      itemPreProcessor,
      versionInfo)

    waitFor(renderer.render("sessionId", obj("session" -> true), obj("item" -> true, "xhtml" -> "<div/>"), bundle, Seq("warning"), Map("query" -> "param"), prodMode, showControls, "check", Json.obj()))
    lazy val captor = capture[Map[String, Any]]
    there was one(jadeEngine).renderJade(any[String], captor)
  }

  "render" should {

    "set renderJade params - showControls == false" in new scope {
      captor.value.get("showControls") must_== Some(false)
    }

    "when showControls == false" should {
      "set renderJade params - js" in new scope {
        captor.value.get("js").get must_== Array.empty
      }

      "set renderJade params" in new scope {
        captor.value.get("showControls") must_== Some(false)
      }

      "set renderJade params - ngModules" in new scope {
        captor.value.get("ngModules") must_== Some("'player-injected'")
      }
    }

    "when showControls == true" should {
      "set renderJade params" in new scope {
        override lazy val showControls = true
        captor.value.get("showControls") must_== Some(true)
      }
      "set renderJade params - js" in new scope {
        override lazy val showControls = true
        captor.value.get("js").get must_== Array("controls.js")
      }

      "set renderJade params - ngModules" in new scope {
        override lazy val showControls = true
        captor.value.get("ngModules") must_== Some("'player-injected','ng.controls'")
      }
    }

    "sets warning" in new scope {
      captor.value.get("warnings") must_== Some("""["warning"]""")
    }

    "sets appName" in new scope {
      captor.value.get("appName") must_== Some("player")
    }

    "set sessionJson" in new scope {
      captor.value.get("sessionJson").get must_== stringify(obj("session" -> obj("session" -> true), "item" -> obj("item" -> true, "xhtml" -> "<div/>")))
    }

    "set queryParams" in new scope {
      captor.value.get("ngServiceLogic").get must_== PlayerServices("player-injected", PlayerEndpoints.session("sessionId"), obj("query" -> "param")).toString
    }
  }
}
