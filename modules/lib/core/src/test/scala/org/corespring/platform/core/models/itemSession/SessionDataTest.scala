package org.corespring.platform.core.models.itemSession

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qti.models.responses.{ StringResponse, ArrayResponse }
import org.corespring.qti.models.{ ItemBody, CorrectResponseSingle, ResponseDeclaration, QtiItem }
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import play.api.libs.json.Json._
import scala.Some
import utils.MockXml

class SessionDataTest extends Specification {

  def createEmptyItemBody = ItemBody(Seq(), Seq())

  def createEmptyQti = QtiItem(Seq(), createEmptyItemBody, Seq())

  def createSession = ItemSession(VersionedId(new ObjectId()))

  def singleResponseDeclaration(id: String = "questionOne", value: String = "value"): Seq[ResponseDeclaration] = Seq(
    ResponseDeclaration(
      identifier = id,
      cardinality = "single",
      baseType = "identifier",
      correctResponse = Some(CorrectResponseSingle(value)),
      mapping = None))

  def createSingleResponseQti = QtiItem(singleResponseDeclaration(), createEmptyItemBody, Seq())

  "new session data" should {
    "be empty for empty qti and session" in {
      val sessionData = SessionData(createEmptyQti, createSession)
      sessionData.correctResponses.length must equalTo(0)
      sessionData.feedbackContents.size must equalTo(0)
    }
  }

  "session data json output" should {

    "work" in {

      val s: ItemSession = createSession
      val qti: QtiItem = createSingleResponseQti
      s.responses = Seq(StringResponse(id = "questionOne", responseValue = "value"))
      s.settings.highlightCorrectResponse = true
      s.finish = Some(new DateTime())
      val json = toJson(SessionData(qti, s))
      val expected = """{"correctResponses":[{"id":"questionOne","value":"value"}],"feedbackContents":{}}"""
      expected must equalTo(stringify(json))
    }
  }

  "session data correct responses" should {
    "only show correct responses if highlightCorrectResponses is true" in {

      val s: ItemSession = createSession
      val qti: QtiItem = createSingleResponseQti
      s.responses = Seq(StringResponse(id = "questionOne", responseValue = "value"))
      SessionData(qti, s).correctResponses.length must equalTo(1)
      s.settings.highlightCorrectResponse = false
      SessionData(qti, s).correctResponses.length must equalTo(0)
    }
  }

  "session data feedback" should {

    val questionId = "questionOne"

    def interactionXml(maxChoices: Int) =
      <choiceInteraction maxChoices={ maxChoices.toString } responseIdentifier={ questionId }>
        <simpleChoice identifier="A">
          A
          <feedbackInline csFeedbackId="cs_1" identifier="A">Super!</feedbackInline>
        </simpleChoice>
        <simpleChoice identifier="B">
          B
          <feedbackInline csFeedbackId="cs_2" identifier="B" defaultFeedback="true"></feedbackInline>
        </simpleChoice>
        <simpleChoice identifier="C">
          C
          <feedbackInline csFeedbackId="cs_3" identifier="C" defaultFeedback="true"></feedbackInline>
        </simpleChoice>
      </choiceInteraction>

    val radioInteraction = interactionXml(1)

    val multipleChoiceInteraction = interactionXml(0)

    val radioInteractionXml = MockXml.createXml(questionId, "single", <value>A</value>, radioInteraction)

    val qti = QtiItem(radioInteractionXml)

    "only show feedback if showFeedback is true and there is a response for that item" in {
      val s: ItemSession = createSession
      s.settings.showFeedback = true
      s.responses = Seq(StringResponse(questionId, responseValue = "A"))
      SessionData(qti, s).feedbackContents.size must equalTo(1)
      s.settings.showFeedback = false
      SessionData(qti, s).feedbackContents.size must equalTo(0)
    }

    "show feedback for correct responses even though it wasn't selected" in {
      val s = createSession
      s.finish = Some(new DateTime())
      s.settings.highlightCorrectResponse = true
      s.settings.showFeedback = true
      //Answer incorrectly
      s.responses = Seq(StringResponse(questionId, responseValue = "B"))
      SessionData(qti, s).feedbackContents.size === 2
    }

    "show feedback for correct responses even though it wasn't selected and correct responses are not shown" in {
      val s = createSession
      s.finish = Some(new DateTime())
      s.settings.highlightCorrectResponse = false
      s.settings.showFeedback = true
      //Answer incorrectly
      s.responses = Seq(StringResponse(questionId, responseValue = "B"))
      SessionData(qti, s).feedbackContents.size === 1
    }

    "show the correct feedback string" in {
      val s: ItemSession = createSession
      s.responses = Seq(StringResponse(questionId, responseValue = "A"))
      s.settings.showFeedback = true

      SessionData(qti, s).feedbackContents.get("cs_1") match {
        case Some(fb) => fb must equalTo("Super!")
        case _ => failure("couldn't find feedback")
      }
    }

    "show default feedback" in {
      val s = createSession
      s.responses = Seq(StringResponse(questionId, responseValue = "B"))
      s.settings.showFeedback = true

      SessionData(qti, s).feedbackContents.get("cs_2") match {
        case Some(fb) => fb must equalTo(MockXml.incorrectResponseFeedback)
        case _ => failure("couldn't find feedback")
      }
    }

    "show feedback for multiple choice items" in {
      val incorrectResponse = createSession
      incorrectResponse.responses = Seq(
        ArrayResponse(questionId, responseValue = Seq("B", "C")))
      incorrectResponse.settings.showFeedback = true
      val multiChoiceXml = MockXml.createXml(questionId,
        "multiple",
        <value>A</value> <value>B</value>,
        multipleChoiceInteraction)

      val qti = QtiItem(multiChoiceXml)
      val data = SessionData(qti, incorrectResponse)
      println(data.feedbackContents)
      data.feedbackContents.size must_== (3)
      data.feedbackContents.get("cs_2").getOrElse("error") === MockXml.correctResponseFeedback
      data.feedbackContents.get("cs_3").getOrElse("error") === MockXml.incorrectResponseFeedback

      val correctResponse = createSession
      correctResponse.settings.highlightCorrectResponse = true
      correctResponse.settings.showFeedback = true
      correctResponse.responses = Seq(
        ArrayResponse(questionId, responseValue = Seq("A", "B")))

      SessionData(qti, correctResponse).feedbackContents.size === 2
    }

    val textWithFeedbackBlocks =
      <p>
        <textEntryInteraction responseIdentifier="id" expectedLength="1"/>
        <feedbackBlock outcomeIdentifier="responses.id.value" identifier="york" csFeedbackId="cs_1">Capitalize</feedbackBlock>
        <feedbackBlock outcomeIdentifier="responses.id.value" identifier="York" csFeedbackId="cs_2">Bingo</feedbackBlock>
        <feedbackBlock outcomeIdentifier="responses.id.value" incorrectResponse="true" csFeedbackId="cs_3">What is this?</feedbackBlock>
      </p>

    val textEntryXml = MockXml.createXml("id",
      "multiple",
      <value>York</value> <value>york</value>,
      textWithFeedbackBlocks)

    val textEntryQti = QtiItem(textEntryXml)

    "show custom correct 1 feedback for text entry interaction" in {
      val s = createSession
      s.settings.showFeedback = true
      s.responses = Seq(
        StringResponse("id", responseValue = "york"))
      val data = SessionData(textEntryQti, s)
      data.feedbackContents.size must_== (1)
      data.feedbackContents.get("cs_1").getOrElse("error") === "Capitalize"
    }

    "show custom correct 2 feedback for text entry interaction" in {
      val s = createSession
      s.settings.showFeedback = true
      s.responses = Seq(StringResponse("id", responseValue = "York"))
      val data = SessionData(textEntryQti, s)
      data.feedbackContents.size must_== (1)
      data.feedbackContents.get("cs_2").getOrElse("error") === "Bingo"
    }

    "show incorrect feedback for text entry interaction" in {
      val s = createSession
      s.settings.showFeedback = true
      s.responses = Seq(StringResponse("id", responseValue = "Some other text"))
      val data = SessionData(textEntryQti, s)
      data.feedbackContents.size must_== (1)
      data.feedbackContents.get("cs_3").getOrElse("error") === "What is this?"
    }

    /**
     * Showing custom feedback for individual items for an order interaction isn't
     * currently possible - we'd need to know if the feedback message was
     * for when the items position was correct or not.
     */
    //"show feedback for ordered items" in { }

  }

  "legacy tests" should {

    "return correct feedback for inline item" in {
      val XML =
        <assessmentItem>
          <correctResponseFeedback>Looking good buddy</correctResponseFeedback>
          <incorrectResponseFeedback>You should rethink this</incorrectResponseFeedback>
          <responseDeclaration identifier="manOnMoon" cardinality="single" baseType="identifier">
            <correctResponse>
              <value>armstrong</value>
            </correctResponse>
          </responseDeclaration>
          <itemBody>
            <inlineChoiceInteraction responseIdentifier="manOnMoon" required="false">
              <inlineChoice identifier="armstrong">
                Neil Armstrong
                <feedbackInline csFeedbackId="1" identifier="armstrong" defaultFeedback="true"/>
              </inlineChoice>
              <inlineChoice identifier="aldrin">
                Buzz Aldrin
                <feedbackInline csFeedbackId="2" identifier="aldrin" defaultFeedback="true"/>
              </inlineChoice>
            </inlineChoiceInteraction>
          </itemBody>
        </assessmentItem>

      val qtiItem = QtiItem(XML)

      val correctResponse = StringResponse("manOnMoon", "armstrong")
      val incorrectResponse = StringResponse("manOnMoon", "aldrin")

      val session = ItemSession(itemId = VersionedId(new ObjectId()), responses = Seq(correctResponse, incorrectResponse))
      session.settings.showFeedback = true
      val data: SessionData = SessionData(qtiItem, session)
      data.feedbackContents.size === 2
      data.feedbackContents.get("1").get === (XML \\ "correctResponseFeedback").text
      data.feedbackContents.get("2").get === (XML \\ "incorrectResponseFeedback").text

    }
  }

  "session data feedback for item with multiple feedback blocks" should {
    "return all the incorrect feedback" in {
      val s: ItemSession = createSession
      s.settings.showFeedback = true
      s.responses = Seq(
        StringResponse("Q_03", "8"),
        StringResponse("Q_04", "8"),
        StringResponse("Q_05", "8"),
        StringResponse("Q_06", "9"))

      val qti = QtiItem(MockXml.load("multiple-feedback-blocks.xml"))
      val sd = SessionData(qti, s)
      println(sd.feedbackContents)
      sd.feedbackContents.size === 4
    }
  }
}
