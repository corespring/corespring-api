package org.corespring.qtiToV2.kds.interactions

import org.corespring.qtiToV2.transformers.{ ItemTransformer, InteractionRuleTransformer }
import org.specs2.mutable.Specification

import scala.xml.transform.RuleTransformer

class RubricBlockTransformerTest extends Specification {

  val prompt = "Hi, I'm a prompt!"

  def qti(prompt: String = prompt) =
    <assessmentItem>
      <responseDeclaration identifier="RESPONSE1" cardinality="single" baseType="string"/>
      <itemBody>
        <extendedTextInteraction responseIdentifier="RESPONSE1" expectedLength="5000">
          <prompt visible="true">{ prompt }</prompt>
        </extendedTextInteraction>
        <rubricBlock view="scorer" label="1">
          This is a rubric block!
        </rubricBlock>
        <rubricBlock view="scorer" label="0">
          This is a another rubric block!
        </rubricBlock>
        <sampleBlock view="scorer" label="1">
          This is a sample block
        </sampleBlock>
      </itemBody>
    </assessmentItem>

  "transform" should {

    val result = new InteractionRuleTransformer(RubricBlockTransformer).transform(qti(), ItemTransformer.EmptyManifest)
      .headOption.getOrElse(throw new Exception("Result was empty"))

    "remove <rubricBlock/>s" in {
      (result \\ "rubricBlock") must beEmpty
    }

    "remove <sampleBlock/>s" in {
      (result \\ "sampleBlock") must beEmpty
    }

  }

  "interactionJs" should {

    val result = RubricBlockTransformer.interactionJs(qti(), ItemTransformer.EmptyManifest)

    "return empty" in {
      result must beEmpty
    }

  }

}
