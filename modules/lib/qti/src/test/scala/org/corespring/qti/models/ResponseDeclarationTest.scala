package org.corespring.qti.models

import org.specs2.mutable._
import org.corespring.qti.models.QtiItem.Correctness

class ResponseDeclarationTest extends Specification {

  val itemBody = ItemBody(
    interactions = Seq(),
    feedbackBlocks = Seq())

  "A single cardinality response declaration" should {
    val xml =
      <responseDeclaration identifier="RESPONSE" cardinality="single" baseType="identifier" exactMatch="true">
        <correctResponse>
          <value>ChoiceA</value>
        </correctResponse>
      </responseDeclaration>

    val responseDeclaration = ResponseDeclaration(xml, itemBody)

    "parse identifier" in {
      if (responseDeclaration.identifier equals "RESPONSE") success else failure
    }

    "parse exactMatch" in {
      responseDeclaration.exactMatch === true
    }

    "single cardinality" in {
      if (responseDeclaration.cardinality == "single") success else failure
    }

    "validate correct response" in {
      if (responseDeclaration.correctResponse.isDefined &&
        responseDeclaration.isCorrect("ChoiceA") == Correctness.Correct) success else failure
    }
  }

  "A multiple cardinality response declaration" should {

    val defaultValue: Int = -2
    val xml =
      <responseDeclaration identifier="RESPONSE" cardinality="multiple" baseType="identifier">
        <correctResponse>
          <value>H</value>
          <value>O</value>
        </correctResponse>
        <mapping defaultValue={ defaultValue.toString } upperBound="2" lowerBound="0">
          <mapEntry mappedValue="1" mapKey="H"></mapEntry>
          <mapEntry mappedValue="2" mapKey="O"></mapEntry>
          <mapEntry mappedValue="-1" mapKey="Cl"></mapEntry>
        </mapping>
      </responseDeclaration>

    val responseDeclaration = ResponseDeclaration(xml, itemBody)

    "provide default for unmapped keys" in {
      if (responseDeclaration.mappedValue("tests") == defaultValue) success else failure
    }

    "calculate response values correctly" in {
      if (responseDeclaration.mappedValue("H") == 1) success else failure
      if (responseDeclaration.mappedValue("O") == 2) success else failure
      if (responseDeclaration.mappedValue("Cl") == -1) success else failure
    }

  }

}
