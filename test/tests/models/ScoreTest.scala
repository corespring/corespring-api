package tests.models

import org.specs2.mutable.Specification
import qti.models.{CorrectResponseSingle, ResponseDeclaration, ItemBody, QtiItem}
import models.{StringItemResponse, ItemResponseOutcome, Score, ItemResponse}
import org.omg.CORBA.IRObject

class ScoreTest extends Specification {

  "score" should {
    val qti = QtiItem(
      itemBody = ItemBody(
        interactions = Seq(),
        feedbackBlocks = Seq()
      ),
      responseDeclarations = Seq(
        ResponseDeclaration(
          identifier = "q1",
          cardinality = "single",
          correctResponse = Some(CorrectResponseSingle("q1Answer")),
          mapping = None)
      ),
      modalFeedbacks = Seq()
    )


    "score single responses" in {
      qti.isCorrect("apple", "hello") must equalTo(false)
      qti.isCorrect("q1", "q1Answer") must equalTo(true)
    }

    "score a sequence of responses with one item" in {
      val seq = Seq( StringItemResponse(id = "q1", responseValue = "q1Answer", outcome = None) )
      val result = Score.scoreResponses(seq, qti)
      val expected = Seq( StringItemResponse(id = "q1", responseValue = "q1Answer", outcome = Some(ItemResponseOutcome(score = 1))) )
      result must equalTo(expected)
    }

    "score a sequence of responses with one item" in {
      val seq = Seq(
        StringItemResponse(id = "q1", responseValue = "q1Answer", outcome = None),
        StringItemResponse(id = "q2", responseValue = "blah", outcome = None) )
      val result = Score.scoreResponses(seq, qti)
      val expected = Seq(
        StringItemResponse(id = "q1", responseValue = "q1Answer", outcome = Some(ItemResponseOutcome(score = 1))),
        StringItemResponse(id = "q2", responseValue = "blah", outcome = Some(ItemResponseOutcome(score = 0))) )
      result must equalTo(expected)
    }
  }
}
