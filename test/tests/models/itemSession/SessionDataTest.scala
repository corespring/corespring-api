package tests.models.itemSession

import org.specs2.mutable.Specification
import models.itemSession.SessionData
import qti.models.{CorrectResponseSingle, ResponseDeclaration, ItemBody, QtiItem}
import models.{StringItemResponse, ItemResponse, ItemSession}
import org.bson.types.ObjectId
import tests.PlaySingleton
import org.joda.time.DateTime

class SessionDataTest extends Specification {

  PlaySingleton.start()

  def EmptyItemBody = ItemBody(Seq(), Seq())

  val emptyQti = QtiItem(Seq(), EmptyItemBody, Seq())
  val session = ItemSession(new ObjectId())

  val singleResponseQti = QtiItem(
    Seq(
      ResponseDeclaration(
        identifier = "questionOne",
        cardinality = "single",
        correctResponse = Some(CorrectResponseSingle("value")),
        mapping = None)
    ),
    EmptyItemBody,
    Seq())

  "new session data" should {
    "be empty for empty qti and session" in {
      val sessionData = SessionData(emptyQti, session)
      sessionData.correctResponses.length must equalTo(0)
      sessionData.feedbackContents.size must equalTo(0)
    }
  }

  "session data" should {
    "only show correct responses for finished session + highlightCorrectResponses is true" in {
      session.responses = Seq( StringItemResponse(id = "questionOne", responseValue = "value") )
      SessionData(singleResponseQti, session).correctResponses.length must equalTo(0)
      session.finish = Some(new DateTime())
      SessionData(singleResponseQti, session).correctResponses.length must equalTo(1)
      session.settings.highlightCorrectResponse = false
      SessionData(singleResponseQti, session).correctResponses.length must equalTo(0)
    }

    "show simple feedback" in {
      session.settings.showFeedback = true
      SessionData(singleResponseQti, session).feedbackContents.size must equalTo(1)
      session.settings.showFeedback = false
      SessionData(singleResponseQti, session).feedbackContents.size must equalTo(0)

      session.settings.showFeedback = true
      singleResponseQti.defaultCorrect = "Correct"

      println(SessionData(singleResponseQti, session))
      SessionData(singleResponseQti, session).feedbackContents.get("questionOne") must beSome("Correct")

    }
  }

}
