package tests.models.itemSession

import org.specs2.mutable.Specification
import models.itemSession.SessionData
import qti.models._
import models.{StringItemResponse, ItemResponse, ItemSession}
import org.bson.types.ObjectId
import tests.PlaySingleton
import org.joda.time.DateTime
import models.StringItemResponse
import scala.Some
import xml.{Node, NodeSeq}

class SessionDataTest extends Specification {

  PlaySingleton.start()

  object xml {
    def qti(responseDeclarations : NodeSeq, choiceInteractions : NodeSeq) = <assessmentItem>
      {responseDeclarations}
      <itemBody>
        {choiceInteractions}
      </itemBody>
    </assessmentItem>

    def rd( id : String, cardinality : String, value : NodeSeq) = {
      <responseDeclaration identifier={id} cardinality={cardinality} baseType="doesnt matter">
        <correctResponse>{value}</correctResponse>
      </responseDeclaration>
    }

    def cr(text : String ) = <value>{text}</value>

    def ci(ri:String, maxChoices : String, simpleChoices : NodeSeq) : Node =
      <choiceInteraction responseIdentifier={ri} maxChoices={maxChoices}>
      {simpleChoices}
    </choiceInteraction>

    def sc(identifier:String, feedbackInline : Node) : Node = <simpleChoice identifier={identifier}>Label
      {feedbackInline}
    </simpleChoice>

    def fi(csFeedbackId:String, defaultFeedback:Boolean, text:String) =
    <feedbackInline csFeedbackId={csFeedbackId} defaultFeedback={defaultFeedback.toString}>{text}</feedbackInline>
  }

  def createEmptyItemBody = ItemBody(Seq(), Seq())

  def createEmptyQti = QtiItem(Seq(), createEmptyItemBody, Seq())

  def createSession = ItemSession(new ObjectId())

  def singleResponseDeclaration(id: String = "questionOne", value: String = "value"): Seq[ResponseDeclaration] = Seq(
    ResponseDeclaration(
      identifier = id,
      cardinality = "single",
      correctResponse = Some(CorrectResponseSingle(value)),
      mapping = None)
  )

  def createSingleResponseQti = QtiItem(singleResponseDeclaration(), createEmptyItemBody, Seq())

  "new session data" should {
    "be empty for empty qti and session" in {
      val sessionData = SessionData(createEmptyQti, createSession)
      sessionData.correctResponses.length must equalTo(0)
      sessionData.feedbackContents.size must equalTo(0)
    }
  }

  "session data" should {
    "only show correct responses for finished session + highlightCorrectResponses is true" in {

      val s : ItemSession = createSession
      val qti : QtiItem = createSingleResponseQti
      s.responses = Seq(StringItemResponse(id = "questionOne", responseValue = "value"))
      SessionData(qti, s).correctResponses.length must equalTo(0)
      s.settings.highlightCorrectResponse = true
      s.finish = Some(new DateTime())
      SessionData(qti, s).correctResponses.length must equalTo(1)
      s.settings.highlightCorrectResponse = false
      SessionData(qti, s).correctResponses.length must equalTo(0)
    }



    "only show feedback if showFeedback is true" in {
      val s : ItemSession = createSession
      val qti : QtiItem = createSingleResponseQti
      s.settings.showFeedback = true
      SessionData(qti, s).feedbackContents.size must equalTo(1)
      s.settings.showFeedback = false
      SessionData(qti, s).feedbackContents.size must equalTo(0)
    }

    /*
    "show default feedback" in {

      val interactions = Seq(
        ChoiceInteraction("questionOne",
          Seq(
            SimpleChoice("answer", "questionOne",
              Some(
                FeedbackInline("csFeedbackId_1", "", "", "", true, false)
              )
            )
          )
        )
      )

      val body = ItemBody(interactions, Seq())
      val qti = QtiItem(singleResponseDeclaration("questionOne", "answer"), body, Seq())
      session.settings.showFeedback = true
      singleResponseQti.defaultCorrect = "Correcto"
      session.responses = Seq(StringItemResponse(id = "questionOne", responseValue = "answer"))
      println(SessionData(singleResponseQti, session))
      SessionData(singleResponseQti, session).feedbackContents.get("csFeedbackId_1") must beSome("Correcto")

    }*/
  }

}
