package controllers.testplayer.qti

import org.specs2.mutable._

class ResponseDeclarationSpec extends Specification {

  "A single cardinality response declaration" should {
    val xml =
      <responseDeclaration identifier="RESPONSE" cardinality="single" baseType="identifier">
        <correctResponse>
          <value>ChoiceA</value>
        </correctResponse>
      </responseDeclaration>

    val responseDeclaration = new ResponseDeclaration(xml)

    "parse identifier" in { if (responseDeclaration.identifier equals "RESPONSE") success else failure }

    "validate correct response" in { if (responseDeclaration.responseFor("ChoiceA") equals "1") success else failure }

    "not validate incorrect response" in {
      if (responseDeclaration.responseFor("ChoiceB") equals "0") success else failure
    }

    "throw an exception when called with multiple choices" in {
      responseDeclaration.responseFor(List("ChoiceA", "ChoiceB")) must throwAn[IllegalArgumentException]
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
        <mapping defaultValue={defaultValue.toString} upperBound="2" lowerBound="0">
          <mapEntry mappedValue="1" mapKey="H"></mapEntry>
          <mapEntry mappedValue="2" mapKey="O"></mapEntry>
          <mapEntry mappedValue="-1" mapKey="Cl"></mapEntry>
        </mapping>
      </responseDeclaration>

    val responseDeclaration = new ResponseDeclaration(xml)

    "provide default for unmapped keys" in {
      if (responseDeclaration.responseFor("test") equals defaultValue.toString) success else failure
    }

    "calculate response of default value for empty choices" in {
      if (responseDeclaration.responseFor(List()) equals defaultValue.toString) success else failure
    }

    "calculate response values correctly" in {
      if (responseDeclaration.responseFor("H") equals "1") success else failure
      if (responseDeclaration.responseFor(List("H", "O")) equals "3") success else failure
      if (responseDeclaration.responseFor(List("H", "O", "Cl")) equals "2") success else failure
    }

  }

}
