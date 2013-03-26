package tests.models.itemSession

import org.specs2.mutable.Specification
import qti.models._
import interactions.ChoiceInteraction
import interactions.choices.SimpleChoice
import models._
import org.omg.CORBA.IRObject
import qti.models.QtiItem.Correctness
import scala.Some
import scala.Some
import models.itemSession.{Score, ItemResponseOutcome, ArrayItemResponse, StringItemResponse}

class ScoreTest extends Specification {

  "score" should {
    val qti = QtiItem(
      itemBody = ItemBody(
        interactions = Seq(
          new ChoiceInteraction("q1",Seq(new SimpleChoice("q1Answer","q1",None),new SimpleChoice("other","q1",None))),
          new ChoiceInteraction("q3",Seq(new SimpleChoice("q3_answer_1","q3",None),new SimpleChoice("q3_answer_3","q3",None)))
        ),
        feedbackBlocks = Seq()
      ),
      responseDeclarations = Seq(
        ResponseDeclaration(
          identifier = "q1",
          cardinality = "single",
          correctResponse = Some(CorrectResponseSingle("q1Answer")),
          mapping = None),

        ResponseDeclaration(
          identifier = "q3",
          cardinality = "multiple",
          correctResponse = Some(CorrectResponseMultiple(Seq("q3_answer_1","q3_answer_3"))),
          mapping = None
          )
      ),
      modalFeedbacks = Seq()
    )


    "score single responses" in {
      qti.isCorrect("apple", "hello") must equalTo(Correctness.Unknown)
      qti.isCorrect("q1", "q1Answer") must equalTo(Correctness.Correct)
      qti.isCorrect("q1", "wrong") must equalTo(Correctness.Incorrect)
    }

    "score a sequence of responses with one item" in {
      val seq = Seq(StringItemResponse(id = "q1", responseValue = "q1Answer", outcome = None))
      val result = Score.scoreResponses(seq, qti)
      val expected = Seq(
        StringItemResponse(
          id = "q1",
          responseValue = "q1Answer",
          outcome = Some(ItemResponseOutcome(1,true,None,Map()))
        )
      )
      result must equalTo(expected)
    }

    "score an array item response" in {
      val seq = Seq(
        ArrayItemResponse(id = "q3", responseValue = Seq("q3_answer_1", "q3_answer_2"), outcome = None)
      )
      val result = Score.scoreResponses(seq, qti)
      val expected = Seq(
        ArrayItemResponse(
          id = "q3",
          responseValue = Seq("q3_answer_1", "q3_answer_2"),
          outcome = Some(ItemResponseOutcome(0,false,None,Map()))
        ))
      result must equalTo(expected)
    }


    "score a sequence of responses with one item" in {
      val seq = Seq(
        StringItemResponse(id = "q1", responseValue = "q1Answer", outcome = None),
        StringItemResponse(id = "q2", responseValue = "blah", outcome = None))
      val result = Score.scoreResponses(seq, qti)
      val expected = Seq(
        StringItemResponse(
          id = "q1",
          responseValue = "q1Answer",
          outcome = Some(ItemResponseOutcome(1,true,None,Map()))
        ),
        StringItemResponse(id = "q2", responseValue = "blah", outcome = None))
      result must equalTo(expected)
    }
  }
}
