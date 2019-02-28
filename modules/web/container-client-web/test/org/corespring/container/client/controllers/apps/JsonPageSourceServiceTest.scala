package org.corespring.container.client.controllers.apps

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.Json._

class JsonPageSourceServiceTest extends Specification {

  val jsJson = obj(
    "src" -> arr("src.js"),
    "dest" -> "dest.js",
    "libs" -> arr("lib.js"),
    "ngModules" -> arr("ng-module"))

  val cssJson = obj(
    "src" -> arr("src.css"),
    "dest" -> "dest.css",
    "libs" -> arr("lib.css"))

  trait scope extends Scope {
    def loadResult: Option[String] = Some("{}")
    def load(path: String) = loadResult
    val config = PageSourceServiceConfig("prefix", false, load)
    lazy val service = new JsonPageSourceService(config)
  }

  trait js extends scope {
    override def loadResult: Option[String] = Some(stringify(jsJson))
  }

  trait css extends scope {
    override def loadResult: Option[String] = Some(stringify(cssJson))
  }

  "loadJs" should {
    "throw an IllegalArgumentException if no path is found" in new js {
      override def loadResult = None
      service.loadJs("test") must throwA[IllegalArgumentException]
    }

    "throw an IllegalArgumentException if report can't be loaded" in new js {
      override def loadResult = Some("hi")
      service.loadJs("test") must throwA[IllegalArgumentException]
    }

    "load NgSourcePaths" in new js {
      service.loadJs("test") must_== SourcePaths.js("prefix", jsJson)
    }
  }

  "loadCss" should {
    "throw an IllegalArgumentException if no path is found" in new css {
      override def loadResult = None
      service.loadCss("test") must throwA[IllegalArgumentException]
    }

    "throw an IllegalArgumentException if report can't be loaded" in new css {
      override def loadResult = Some("hi")
      service.loadCss("test") must throwA[IllegalArgumentException]
    }

    "load CssSourcePaths" in new css {
      service.loadCss("test") must_== SourcePaths.css("prefix", cssJson)
    }
  }
}
