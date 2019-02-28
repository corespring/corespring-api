package org.corespring.container.client.pages.engine

import org.joda.time.DateTime
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.Mode

class JadeEngineTest extends Specification {

  trait scope extends Scope {

    def mode = Mode.Dev
    def loadString(s: String) = Some("doctype html")
    def lastModifiedFromPath(s: String) = Some(DateTime.now.getMillis)
    val config = JadeEngineConfig("root", mode, loadString, lastModifiedFromPath)
    val engine = new JadeEngine(config)
  }

  "renderJade" should {

    "render simple jade" in new scope {
      val html = engine.renderJade("test", Map.empty)
      html.toString must_== "<!DOCTYPE html>"
    }

    "render jade with params" in new scope {
      override def loadString(s: String) = Some(
        """
          |h1= name
        """.stripMargin)
      val html = engine.renderJade("test", Map("name" -> "ed"))
      html.toString must_== "<h1>ed</h1>"
    }

    "recompile jade if mode is dev" in new scope {
      var called = false
      override def loadString(s: String) = {
        Some(
          if (called) {
            "h1 Second"
          } else {
            called = true
            "h1 First"
          })
      }
      engine.renderJade("test", Map.empty).toString must_== "<h1>First</h1>"
      engine.renderJade("test", Map.empty).toString must_== "<h1>Second</h1>"
    }

    "not recompile jade if mode is prod" in new scope {
      override val mode = Mode.Prod
      var called = false
      override def loadString(s: String) = {
        Some(
          if (called) {
            "h1 Second"
          } else {
            called = true
            "h1 First"
          })
      }
      engine.renderJade("test", Map.empty).toString must_== "<h1>First</h1>"
      engine.renderJade("test", Map.empty).toString must_== "<h1>First</h1>"
    }
  }
}
