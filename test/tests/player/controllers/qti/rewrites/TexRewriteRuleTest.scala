package tests.player.controllers.qti.rewrites

import org.specs2.mutable.Specification
import player.controllers.qti.rewrites.TexRewriteRule
import scala.xml._
import org.apache.commons.lang3.StringEscapeUtils

class TexRewriteRuleTest extends Specification {

  import TexRewriteRule._

  val tex = "latex source"

  "transform" should {

    "do nothing for non-tex node" in {
      val input: Seq[Node] = <node></node>
      val output = TexRewriteRule.transform(input)
      output must be equalTo(input)
    }

    "transform <tex/> into Text node with default inline" in {
      val input = <tex>{tex}</tex>
      val output = TexRewriteRule.transform(input)
      output must be equalTo defaultTexTransform(input)
    }

    "transform <tex inline='true'/> into inline tex" in {
      val input = <tex inline="true">{tex}</tex>
      val output = TexRewriteRule.transform(input)
      output must be equalTo inlineTex(input)
    }

    "transform <tex inline='false'/> into block tex" in {
      val input = <tex inline="false">{tex}</tex>
      val output = TexRewriteRule.transform(input)
      output must be equalTo blockTex(input)
    }

  }

  "blockTex" should {

    "return children surrounded with $$" in {
      val input = <tex>{tex}</tex>
      val output = blockTex(input)
      output.text must be equalTo s"$$$$$tex$$$$"
    }

    "return children HTML encoded" in {
      val tex = "3 &gt; 4"
      val input = <tex>{tex}</tex>
      val output = blockTex(input)
      output.toString must be equalTo s"$$$$${StringEscapeUtils.escapeHtml4(tex)}$$$$"
    }

  }

  "inlineTex" should {

    "return children surrounded with \\( \\)" in {
      val input = <tex>{tex}</tex>
      val output = inlineTex(input)
      output.text must be equalTo s"\\($tex\\)"
    }

    "return children HTML encoded" in {
      val tex = "3 &gt; 4"
      val input = <tex>{tex}</tex>
      val output = inlineTex(input)
      output.toString must be equalTo s"\\(${StringEscapeUtils.escapeHtml4(tex)}\\)"
    }

  }

}
