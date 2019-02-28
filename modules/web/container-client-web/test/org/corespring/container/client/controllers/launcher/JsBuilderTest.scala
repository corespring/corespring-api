package org.corespring.container.client.controllers.launcher

import org.corespring.container.client.io.ResourcePath
import org.corespring.container.client.views.txt.js.ServerLibraryWrapper
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.{ Fragments, Scope }
import play.api.libs.json.Json

class JsBuilderTest extends Specification with Mockito {

  trait Loader {
    def load(s: String): Option[String]
  }

  trait scope extends Scope {

    lazy val loader = {
      val m = mock[ResourcePath]
      m.loadPath(any[String]).answers { (key: Any) => Some(s"//${key.asInstanceOf[String]}") }
      m
    }

    val builder = new JsBuilder(loader)
    def path(s: String) = s"//container-client/js/player-launcher/$s.js"
    val jsString = builder.buildJs("url", Seq("file.js"), Json.obj("opts" -> "opts"), "bootstrap", Map("a" -> "apple"))
  }

  "buildJs" should {

    def assert(key: String, testPath: Option[String] = None): Fragments = {
      s"add $key" in new scope {
        val actualPath = testPath.getOrElse(path(key))
        val jsLib = ServerLibraryWrapper(key, actualPath).toString
        jsString.contains(jsLib) must_== true
      }
    }

    assert("logger")
    assert("callback-utils")
    assert("error-codes")
    assert("instance")
    assert("client-launcher")
    assert("url-builder")
    assert("object-id")
    assert("draft-id")

    "contains bootstrap" in new scope {
      jsString.contains("bootstrap")
    }

    "only loads core libs once" in new scope {

      (1 to 10).foreach { _ =>
        builder.buildJs("url", Seq("file.ls"), Json.obj("opts" -> "opts"), "bootstrap", Map("a" -> "b"))
      }

      forall(Seq(
        "logger",
        "callback-utils",
        "error-codes",
        "instance",
        "client-launcher",
        "url-builder",
        "object-id",
        "draft-id")) { (k: String) =>
        there was one(loader).loadPath(s"container-client/js/player-launcher/$k.js")
      }

    }
  }
}
