package org.corespring.container.client.pages

import org.corespring.container.client.component.Bundle
import org.corespring.container.client.controllers.apps.{ CssSourcePaths, NgSourcePaths, PageSourceService }
import org.corespring.container.client.pages.processing.AssetPathProcessor
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class CoreRendererTest extends Specification with Mockito {

  case class SimpleBundle(js: Seq[String], css: Seq[String]) extends Bundle

  trait scope extends Scope with CoreRenderer {

    override lazy val assetPathProcessor: AssetPathProcessor = {
      val m = mock[AssetPathProcessor]
      m.process(any[String]) answers (s => s.asInstanceOf[String])
      m
    }

    override lazy val pageSourceService: PageSourceService = {
      val m = mock[PageSourceService]
      m.loadJs(any[String]) returns {
        NgSourcePaths(Seq("src-1.js"), "dest.js", Seq("other-libs-1.js"), Nil)
      }
      m.loadCss(any[String]) returns {
        CssSourcePaths(Seq("src-1.css"), "dest.css", Seq("other-libs-1.css"))
      }
      m
    }

    override lazy val name: String = "test"
  }

  "prepareJsCss" should {

    "when prodMode is true return prod js/css" in new scope {
      val bundle = SimpleBundle(Seq("a.js"), Seq("a.css"))
      val (js, css) = prepareJsCss(true, bundle)
      js must_== Seq("other-libs-1.js", "dest.js", "a.js")
      css must_== Seq("other-libs-1.css", "dest.css", "a.css")
    }

    "when prodMode is false return expanded js/css" in new scope {
      val bundle = SimpleBundle(Seq("a.js"), Seq("a.css"))
      val (js, css) = prepareJsCss(false, bundle)
      js must_== Seq("other-libs-1.js", "src-1.js", "a.js")
      css must_== Seq("other-libs-1.css", "src-1.css", "a.css")
    }
  }

  "jsArrayString" should {

    "make a js array string" in new scope {
      jsArrayString(Seq("a", "b")) must_== "'a','b'"
    }
  }

}
