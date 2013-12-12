package org.corespring.qti.models

import io.Codec
import java.nio.charset.Charset
import org.corespring.qti.models.QtiItem.Correctness
import org.corespring.qti.models.interactions.{ FeedbackInline, TextEntryInteraction, Interaction }
import org.specs2.mutable.Specification
import scala.xml._
import org.corespring.qti.helpers.MockXml
import scala.Some

class QtiItemTest extends Specification {

  val AllItems = QtiItem(MockXml.AllItems)

  def xml(identifier: String, cardinality: String, values: NodeSeq, interaction: NodeSeq = <none/>): Elem = MockXml.createXml(identifier, cardinality, values, interaction)

  "QtiItem" should {

    val textEntryOne = QtiItem(xml(
      "id",
      "single",
      <value>14</value> <value>Fourteen</value>,
        <textEntryInteraction responseIdentifier="id" expectedLength="1"/>))

    val selectText = QtiItem(xml(
      "id",
      "single",
      <value>14</value>,
        <selectTextInteraction responseIdentifier="id" selectionType="sentence" minSelections="2" maxSelections="2"/>))

    val single = QtiItem(xml("id", "single", <value>1</value>))
    val multiple = QtiItem(xml("id", "multiple", <value>1</value> <value>2</value>))
    val ordered = QtiItem(xml("id", "ordered", <value>1</value> <value>2</value>))
    val targeted = QtiItem(xml("id", "targeted", <value identifier="id1"><value>apple</value></value> <value identifier="id2"><value>pear</value></value>))

    def assertParse(item: QtiItem, t: Class[_]): Boolean = {
      val response = item.responseDeclarations(0).correctResponse.get
      item.responseDeclarations.size == 1 &&
        t.getName.startsWith(response.getClass.getName)
    }

    "parse a response that is for a textEntryInteraction as a CorrectResponseAny" in {
      assertParse(textEntryOne, CorrectResponseAny.getClass) must equalTo(true)
    }

    "parse a response that is for a selectTextInteraction as a CorrectResponseAny" in {
      assertParse(selectText, CorrectResponseAny.getClass) must equalTo(true)
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

    "parse a correct response targeted" in {
      assertParse(targeted, CorrectResponseTargeted.getClass)
    }

    "parse an inline choice interaction even though its nested" in {
      val path = "src/test/resources/test/mockXml/inline-choice-interaction-in-p.xml"

      val s = io.Source.fromFile(path)(new Codec(Charset.forName("UTF-8"))).mkString
      val xml = XML.loadString(s)

      val qtiItem = QtiItem(xml)

      qtiItem.itemBody.interactions.size must equalTo(1)
    }
  }

  "QtiItem xml parsing" should {
    val item = QtiItem(MockXml.load("multiple-feedback-blocks.xml"))

    def interactionMatchesExpectation(i: Interaction, expectedValues: Seq[String]): Boolean = i match {
      case TextEntryInteraction(id, length, blocks) => {
        if (blocks.length != 2) return false
        if (!blocks(0).content.contains(expectedValues(0))) return false
        if (!blocks(1).content.contains(expectedValues(1))) return false
        true
      }
      case _ => false
    }

    "parse xml with multiple feedback ids" in {
      item.getFeedback("Q_03", "-5").isDefined === true
      item.getFeedback("Q_03", "incorrect answer").isDefined === true
      item.getFeedback("Q_04", "7.5").isDefined === true
      item.getFeedback("Q_04", "incorrect answer").isDefined === true
      item.getFeedback("Q_05", "-2").isDefined === true
      item.getFeedback("Q_05", "incorrect answer").isDefined === true
      item.getFeedback("Q_06", "8").isDefined === true
      item.getFeedback("Q_06", "incorrect answer").isDefined === true
    }

    "find a feedback block for an incorrect response" in {
      item.getFeedback("Q_03", "8") match {
        case Some(FeedbackInline(csFeedbackId, outcomeId, _, _, _, _, _)) => {
          csFeedbackId === "2"
          outcomeId === "Q_03"
        }
        case _ => failure("couldn't find item")
      }
    }

    "find all interactions" in {
      val q3: Interaction = item.itemBody.getInteraction("Q_03").get
      interactionMatchesExpectation(q3, Seq("Q_03 : -5", "Q_03 : incorrect")) === true

      val q4: Interaction = item.itemBody.getInteraction("Q_04").get
      interactionMatchesExpectation(q4, Seq("Q_04 : 7.5", "Q_04 : incorrect")) === true

      val q6: Interaction = item.itemBody.getInteraction("Q_06").get
      interactionMatchesExpectation(q6, Seq("Q_06 : 8", "Q_06 : incorrect")) === true
    }

    "is correct response applicable is false for text interactions" in {

      val textEntryXml = MockXml.createXml("1",
        "multiple",
        <value>six</value><value>6</value>,
        <textEntryInteraction responseIdentifier="1" expectedLength="4"></textEntryInteraction>)

      val qti = QtiItem(textEntryXml)

      qti.isCorrectResponseApplicable("1") === false
    }

  }

  "QtiItem iscorrect" should {
    "return correct scores" in {

      val testXml = <assessmentItem>
        <responseDeclaration identifier="q1" cardinality="single" baseType="identifier">
          <correctResponse>
            <value>q1Answer</value>
          </correctResponse>
        </responseDeclaration>
        <itemBody>
          <choiceInteraction responseIdentifier="q1"></choiceInteraction>
        </itemBody>
      </assessmentItem>

      val qti = QtiItem(testXml)
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

  "return correct when ignoring whitespace" in {
    val identifier = "q1"
    val correctResponse = "q1answer"
    val testXml =
      <assessmentItem>
        <responseDeclaration identifier={identifier} cardinality="single" baseType="identifier" exactMatch="false">
          <correctResponse>
            <value>{correctResponse}</value>
          </correctResponse>
        </responseDeclaration>
        <itemBody>
          <choiceInteraction responseIdentifier={identifier}></choiceInteraction>
        </itemBody>
      </assessmentItem>

    val qti = QtiItem(testXml)

    qti.isCorrect("q1", s"""   $correctResponse   """) must equalTo(Correctness.Correct)
  }

  "return correct when correct answer contains whitespace" in {
    val identifier = "Q_01"
    val correctResponse = "correct answer"
    val testXml =
      <assessmentItem>
        <responseDeclaration identifier={identifier} cardinality="single" baseType="identifier" exactMatch="false">
          <correctResponse>
            <value>{correctResponse}</value>
          </correctResponse>
        </responseDeclaration>
        <itemBody>
          <choiceInteraction responseIdentifier={identifier}></choiceInteraction>
          <feedbackBlock outcomeIdentifier='responses.Q_01.value' identifier='correct answer'>
            <div class='feedback-block-correct'>Nice work, that's correct!</div>
          </feedbackBlock>
          <feedbackBlock outcomeIdentifier='responses.Q_01.value' incorrectResponse='true'>
            <div class='feedback-block-incorrect'>Good try, but the answer is CORRECT ANSWER.</div>
          </feedbackBlock>
        </itemBody>
      </assessmentItem>

    val qti = QtiItem(testXml)

    qti.isCorrect("Q_01", """correct answer""") must equalTo(Correctness.Correct)
  }

  "QtiItem get feedback" should {
    val feedbackXml = xml("id", "single",
      <value>1</value>,
      <choiceInteraction responseIdentifier="id">
        <simpleChoice identifier="1">
          a
          <feedbackInline csFeedbackId="cs_1" identifier="1">You are correct</feedbackInline>
        </simpleChoice>
        <simpleChoice identifier="2">
          a
          <feedbackInline csFeedbackId="cs_2" identifier="2">You are incorrect</feedbackInline>
        </simpleChoice>
      </choiceInteraction>
        <feedbackBlock outcomeIdentifier="responses.id.value" identifier="3" csFeedbackId="cs_3" showHide="show">
          cs_3 feedback text
        </feedbackBlock>)

    "find simple choice feedback inline" in {
      QtiItem(feedbackXml).getFeedback("id", "1") must not beNone
    }

    "find feedback blocks" in {
      QtiItem(feedbackXml).getFeedback("id", "3") must not beNone
    }

  }

}

class ItemBodyTest extends Specification {

  "ItemBody" should {
    "find a TextEntryInteraction" in {

      val body = (MockXml.AllItems \ "itemBody").head

      val itemBody = ItemBody(body)
      itemBody.getInteraction("winterDiscontent") must not beNone

      itemBody.getInteraction("winterDiscontent") match {
        case Some(i) => {
          val ti = i.asInstanceOf[TextEntryInteraction]
          ti.feedbackBlocks.length === 3
        }
        case _ => failure("can't find text entry interaction")
      }
    }
  }

}
