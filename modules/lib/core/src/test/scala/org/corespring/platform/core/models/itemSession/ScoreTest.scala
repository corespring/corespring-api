package org.corespring.platform.core.models.itemSession

import org.corespring.qti.models.QtiItem.Correctness
import org.corespring.qti.models._
import org.corespring.qti.models.interactions.ChoiceInteraction
import org.corespring.qti.models.interactions.choices.SimpleChoice
import org.corespring.qti.models.responses.{ ResponseOutcome, StringResponse, ArrayResponse }
import org.specs2.mutable.Specification
import scala.Some

class ScoreTest extends Specification {

  "score" should {
    val qti = QtiItem(
      itemBody = ItemBody(
        interactions = Seq(
          new ChoiceInteraction("q1", Seq(new SimpleChoice("q1Answer", "q1", None), new SimpleChoice("other", "q1", None))),
          new ChoiceInteraction("q3", Seq(new SimpleChoice("q3_answer_1", "q3", None), new SimpleChoice("q3_answer_3", "q3", None)))),
        feedbackBlocks = Seq()),
      responseDeclarations = Seq(
        ResponseDeclaration(
          identifier = "q1",
          cardinality = "single",
          baseType = "identifier",
          exactMatch = true,
          correctResponse = Some(CorrectResponseSingle("q1Answer")),
          mapping = None),

        ResponseDeclaration(
          identifier = "q3",
          cardinality = "multiple",
          baseType = "identifier",
          exactMatch = true,
          correctResponse = Some(CorrectResponseMultiple(Seq("q3_answer_1", "q3_answer_3"))),
          mapping = None)),
      modalFeedbacks = Seq())

    "score single responses" in {
      qti.isCorrect("apple", "hello") must equalTo(Correctness.Unknown)
      qti.isCorrect("q1", "q1Answer") must equalTo(Correctness.Correct)
      qti.isCorrect("q1", "wrong") must equalTo(Correctness.Incorrect)
    }

    "score a sequence of responses with one item" in {
      val seq = Seq(StringResponse(id = "q1", responseValue = "q1Answer", outcome = None))
      val result = Score.scoreResponses(seq, qti)
      val expected = Seq(
        StringResponse(
          id = "q1",
          responseValue = "q1Answer",
          outcome = Some(ResponseOutcome(1, true, None, Map()))))
      result must equalTo(expected)
    }

    "score an array item response" in {
      val seq = Seq(
        ArrayResponse(id = "q3", responseValue = Seq("q3_answer_1", "q3_answer_2"), outcome = None))
      val result = Score.scoreResponses(seq, qti)
      val expected = Seq(
        ArrayResponse(
          id = "q3",
          responseValue = Seq("q3_answer_1", "q3_answer_2"),
          outcome = Some(ResponseOutcome(0, false, None, Map()))))
      result must equalTo(expected)
    }

    "score a sequence of responses with one item" in {
      val seq = Seq(
        StringResponse(id = "q1", responseValue = "q1Answer", outcome = None),
        StringResponse(id = "q2", responseValue = "blah", outcome = None))
      val result = Score.scoreResponses(seq, qti)
      val expected = Seq(
        StringResponse(
          id = "q1",
          responseValue = "q1Answer",
          outcome = Some(ResponseOutcome(1, true, None, Map()))),
        StringResponse(id = "q2", responseValue = "blah", outcome = None))
      result must equalTo(expected)
    }
  }
}
