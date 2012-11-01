package tests.qti.models

import org.specs2.mutable.Specification
import io.Codec
import java.nio.charset.Charset
import scala.xml.XML
import qti.models._
import scala.xml._
import utils.MockXml
import qti.models.QtiItem.Correctness

class QtiItemTest extends Specification {


  val AllItems = QtiItem(MockXml.AllItems)

  def xml(cardinality: String, values: NodeSeq, interaction: Elem = <none/>): Elem = {

    <assessmentItem>
      <responseDeclaration identifier="Q_01" cardinality={cardinality} baseType="string">
        <correctResponse>
          {values}
        </correctResponse>
      </responseDeclaration>
      <itemBody>
        {interaction}
      </itemBody>
    </assessmentItem>

  }

  "QtiItem" should {

    val textEntryOne = QtiItem(xml(
      "single",
      <value>14</value> <value>Fourteen</value>,
        <textEntryInteraction responseIdentifier="Q_01" expectedLength="1"/>))

    val single = QtiItem(xml("single", <value>1</value>))
    val multiple = QtiItem(xml("multiple", <value>1</value> <value>2</value>))
    val ordered = QtiItem(xml("ordered", <value>1</value> <value>2</value>))

    def assertParse(item: QtiItem, t: Class[_]): Boolean = {
      val response = item.responseDeclarations(0).correctResponse.get
      item.responseDeclarations.size == 1 &&
        t.getName.startsWith(response.getClass.getName)
    }

    "parse a response that is for a textEntryInteraction as a CorrectResponseAny" in {
      assertParse(textEntryOne, CorrectResponseAny.getClass) must equalTo(true)
    }

    "parse a correctResponse single" in {
      assertParse(single, CorrectResponseSingle.getClass)
    }

    "parse a correct response multiple" in {
      assertParse(multiple, CorrectResponseMultiple.getClass)
    }

    "parse a correct response ordered" in {
      assertParse(ordered, CorrectResponseOrdered.getClass)
    }

    "parse an inline choice interaction even though its nested" in {
      val path = "test/mockXml/inline-choice-interaction-in-p.xml"

      val s = io.Source.fromFile(path)(new Codec(Charset.forName("UTF-8"))).mkString
      val xml = XML.loadString(s)

      val qtiItem = QtiItem(xml)

      qtiItem.itemBody.interactions.size must equalTo(1)
    }
  }

  "QtiItem iscorrect" should {
    "return correct scores" in {

      val xml = <assessmentItem>
        <responseDeclaration identifier="q1" cardinality="single" baseType="identifier">
          <correctResponse>
            <value>q1Answer</value>
          </correctResponse>
        </responseDeclaration>
        <itemBody>
          <choiceInteraction responseIdentifier="q1"></choiceInteraction>
        </itemBody>
      </assessmentItem>

      val qti = QtiItem(xml)
      qti.isCorrect("q1", "q1Answer") must equalTo(Correctness.Correct)
    }

    "return correct for multiple choice" in {
      AllItems.isCorrect("rainbowColors", "blue,violet,red") must equalTo(Correctness.Correct)
      AllItems.isCorrect("rainbowColors", "violet,red,blue") must equalTo(Correctness.Correct)
    }

    "return correct for text interaction" in {
      AllItems.isCorrect("winterDiscontent", "york") must equalTo(Correctness.Correct)
    }

    "return unknown for long answer" in {
      AllItems.isCorrect("longAnswer", "anything") must equalTo(Correctness.Unknown)
    }
  }

}


class ItemBodyTest extends Specification {

  "ItemBody" should {
    "find a TextEntryInteraction" in {

      val body = (MockXml.AllItems \ "itemBody").head
      println(body)

      val itemBody = ItemBody(body)
      itemBody.getInteraction("winterDiscontent") must not beNone
    }
  }

}
class CorrectResponseTest extends Specification {
  "CorrectResponse" should {
    "single - return correct" in {
      CorrectResponseSingle("A").isCorrect("A") must equalTo(true)
      CorrectResponseSingle("A").isCorrect("B") must equalTo(false)
    }

    "multiple - return correct" in {
      CorrectResponseMultiple(Seq("A", "B")).isCorrect("A,B") must equalTo(true)
      CorrectResponseMultiple(Seq("A", "B")).isCorrect("B,A") must equalTo(true)
      CorrectResponseMultiple(Seq("A", "B")).isCorrect("A") must equalTo(false)
      CorrectResponseMultiple(Seq("A", "B")).isCorrect("B") must equalTo(false)
      CorrectResponseMultiple(Seq("A", "B")).isCorrect("A,B,C") must equalTo(false)
      CorrectResponseMultiple(Seq("A", "B")).isCorrect("") must equalTo(false)
    }

    "any - return correct" in {
      CorrectResponseAny(Seq("A", "B", "C")).isCorrect("A") must equalTo(true)
      CorrectResponseAny(Seq("A", "B", "C")).isCorrect("B") must equalTo(true)
      CorrectResponseAny(Seq("A", "B", "C")).isCorrect("C") must equalTo(true)
      CorrectResponseAny(Seq("A", "B", "C")).isCorrect("D") must equalTo(false)
      CorrectResponseAny(Seq("A", "B", "C")).isCorrect("") must equalTo(false)
    }

    "ordered - return correct" in {
      CorrectResponseOrdered(Seq("A", "B", "C")).isCorrect("A,B,C") must equalTo(true)
      CorrectResponseOrdered(Seq("A", "B", "C")).isCorrect("B,A,C") must equalTo(false)
      CorrectResponseOrdered(Seq("A", "B", "C")).isCorrect("C,A,B") must equalTo(false)
      CorrectResponseOrdered(Seq("A", "B", "C")).isCorrect("A,B") must equalTo(false)
      CorrectResponseOrdered(Seq("A", "B", "C")).isCorrect("D") must equalTo(false)
      CorrectResponseOrdered(Seq("A", "B", "C")).isCorrect("") must equalTo(false)
    }

  }
}
