package tests.qti.models

import org.specs2.mutable.Specification
import io.Codec
import java.nio.charset.Charset
import scala.xml.XML
import qti.models.{CorrectResponse, CorrectResponseMultiple, QtiItem}

class QtiItemTest extends Specification{


  "QtiItem" should {
    "parse a response that has a single cardinality but has multiple values as a CorrectResponseMutliple" in {

      val SINGLE_RESPONSE_WITH_MULTIPLE_VALUES = <assessmentItem >
        <responseDeclaration identifier="Q_01" cardinality="single" baseType="string">
          <correctResponse>
            <value>14</value>
            <value>fourteen</value>
            <value>14.0</value>
            <value>Fourteen</value>
            <value>FOURTEEN</value>
          </correctResponse>
        </responseDeclaration>
        <itemBody>
           <textEntryInteraction responseIdentifier="Q_01" expectedLength="5"/>
          <feedbackBlock
          outcomeIdentifier="responses.Q_01.value"
          identifier="fourteen">
            <div class="feedback-block-correct">Nice work, that's correct!</div>
          </feedbackBlock>
        </itemBody>
      </assessmentItem>

      val item = QtiItem(SINGLE_RESPONSE_WITH_MULTIPLE_VALUES)
      item.responseDeclarations.size must equalTo(1)
      val response = item.responseDeclarations(0).correctResponse.get
      response.isInstanceOf[CorrectResponseMultiple] must be equalTo(true)
    }

    "parse an inline choice interaction even though its nested" in {
      val path = "test/mockXml/inline-choice-interaction-in-p.xml"
      val s = io.Source.fromFile(path)(new Codec(Charset.forName("UTF-8"))).mkString
      val xml = XML.loadString(s)

      val qtiItem = QtiItem(xml)

      qtiItem.itemBody.interactions.size must equalTo(1)

    }
  }

}

class CorrectResponseTest extends Specification{
  "CorrectResponse" should {
    "return correct" in {

      val xml = <correctResponse>
        <value>A</value>
        </correctResponse>

      CorrectResponse(xml, "single").isCorrect("A") must equalTo(true)
      CorrectResponse(xml, "single").isCorrect("B") must equalTo(false)
    }

    "only return correct for a checkbox if all items are selected" in {
      val xml = <correctResponse>
        <value>blue</value>
        <value>violet</value>
        <value>red</value>
      </correctResponse>

      //These assertions are incorrect - they'll be changed shortly.
      CorrectResponse(xml, "multiple").isCorrect("blue") must equalTo(true)
      CorrectResponse(xml, "multiple").isCorrect("violet") must equalTo(true)
      CorrectResponse(xml, "multiple").isCorrect("red") must equalTo(true)
      CorrectResponse(xml, "multiple").isCorrect("blue,violet,red,purple") must equalTo(false)
    }


  }
}
