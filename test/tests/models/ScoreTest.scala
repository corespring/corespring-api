package tests.models

import org.specs2.mutable.Specification
import qti.models.{CorrectResponseSingle, ResponseDeclaration, ItemBody, QtiItem}
import models.{ItemResponseOutcome, Score, ItemResponse}
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
      val seq = Seq( ItemResponse(id = "q1", value = "q1Answer", outcome = None) )
      val result = Score.scoreResponses(seq, qti)
      val expected = Seq( ItemResponse(id = "q1", value = "q1Answer", outcome = Some(ItemResponseOutcome(score = 1))) )
      result must equalTo(expected)
    }

    "score a sequence of responses with one item" in {
      val seq = Seq(
        ItemResponse(id = "q1", value = "q1Answer", outcome = None),
        ItemResponse(id = "q2", value = "blah", outcome = None) )
      val result = Score.scoreResponses(seq, qti)
      val expected = Seq(
        ItemResponse(id = "q1", value = "q1Answer", outcome = Some(ItemResponseOutcome(score = 1))),
        ItemResponse(id = "q2", value = "blah", outcome = Some(ItemResponseOutcome(score = 0))) )
      result must equalTo(expected)
    }
  }
}
