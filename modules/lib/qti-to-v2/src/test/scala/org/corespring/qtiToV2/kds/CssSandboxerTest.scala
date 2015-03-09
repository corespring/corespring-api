package org.corespring.qtiToV2.kds

import org.specs2.mutable.Specification
import play.api.libs.json.Json

import scala.xml.{Node, Unparsed}
import scala.xml.transform.{RewriteRule, RuleTransformer}

class CssSandboxerTest extends Specification {

  "sandbox" should {

    "prepend selector to single rule" in {
      val css = "h1 { font-weight:bold; }"
      CssSandboxer.sandbox(css, ".qti") must be equalTo(s".qti $css")
    }

    "prepend selector to multiple rules" in {
      val css = Seq("h1 { font-weight:bold; }", "h2 { text-transform:uppercase; }")
      CssSandboxer.sandbox(css.mkString(" "), ".qti") must be equalTo(css.map(rule => s".qti $rule").mkString(" "))
    }

    "prepend selector to rules inside media queries" in {
      val css = "@media screen and (max-width:300px) { h1 { font-weight:bold; } }"
      CssSandboxer.sandbox(css, ".qti") must be equalTo("@media screen and (max-width:300px) { .qti h1 { font-weight:bold; } }")
    }

  }

}
