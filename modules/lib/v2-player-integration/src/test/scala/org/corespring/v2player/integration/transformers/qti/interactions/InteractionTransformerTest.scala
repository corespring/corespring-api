package org.corespring.v2player.integration.transformers.qti.interactions

import org.specs2.mutable.Specification

class InteractionTransformerTest extends Specification with InteractionTransformer {

  val responseIdentifier = "Q_01"

  val responseDeclarationNode = <responseDeclaration identifier={responseIdentifier} />

  val interaction = <node responseIdentifier={responseIdentifier} />
  val interactionWithNoIdentifier = <node/>
  val interactionWithoutResponseDeclaration = <node responseIdentifier="not-found"/>

  val qti =
    <assessmentItem>
      {responseDeclarationNode}
      <itemBody>
        { Seq(interaction, interactionWithNoIdentifier, interactionWithoutResponseDeclaration) }
      </itemBody>
    </assessmentItem>

  "responseDeclaration" should {

    "return <responseDeclaration/> corresponding to identifier" in {
      responseDeclaration(interaction, qti) must be equalTo responseDeclarationNode
    }

    "throw IllegalArgumentException when there is no identifier" in {
      responseDeclaration(interactionWithNoIdentifier, qti) must throwAn[IllegalArgumentException]
    }

    "throw IllegalArgumentException when there is no corresponding <responseDeclaration/>" in {
      responseDeclaration(interactionWithoutResponseDeclaration, qti) must throwAn[IllegalArgumentException]
    }

  }

}
