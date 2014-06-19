package org.corespring.qtiToV2.interactions

import org.specs2.mutable.Specification

import scala.xml.Node
import scala.xml.transform.RuleTransformer

class TexTransformerTest extends Specification {

  "TexTransformer" should {

    implicit class TexNodeHelper(node: Node) {
      def inQti =
        <assessmentItem>
          <itemBody>{ node }</itemBody>
        </assessmentItem>

      def transformed = new RuleTransformer(TexTransformer).transform(node)
    }

    val tex = "This is my latex!"

    def texNode(tex: String, inline: Option[Boolean] = None) = inline match {
      case Some(inlineValue) => <tex inline={ inlineValue.toString }>{ tex }</tex>
      case _ => <tex>{ tex }</tex>
    }

    def inlineOutput(tex: String) = s"\\($tex\\)"
    def nonInlineOutput(tex: String) = s"$$$$$tex$$$$"

    "should remove <tex/>" in {
      texNode(tex).inQti.transformed \\ "tex" must beEmpty
    }

    "should replace <tex/> body with inline by default" in {
      texNode(tex).inQti.transformed.text.trim must equalTo(inlineOutput(tex))
    }

    "should replace <tex/> body with non-inline when inline='false'" in {
      texNode(tex, inline = Some(false)).inQti.transformed.text.trim must equalTo(nonInlineOutput(tex))
    }

    "should replace <tex/> body with inline when inline='true'" in {
      texNode(tex, inline = Some(true)).inQti.transformed.text.trim must equalTo(inlineOutput(tex))
    }

  }

}
