package org.corespring.qti.models

import org.specs2.mutable.Specification


class CorrectResponseOrderedTest extends Specification {

  val correctResponses = Seq("2x+y", "y+2x", "3z-5")
  val correctResponse = correctResponses.mkString(",")
  val correctResponseWithWhitespace = correctResponses.map(_.toCharArray.mkString(" ")).mkString(" , ")
  val correctResponsesShuffled = correctResponses.reverse
  val correctResponseShuffled = correctResponsesShuffled.mkString(",")

  "isCorrect" should {
    val correctResponseOrdered = CorrectResponseOrdered(correctResponses)

    "return true for correct" in { correctResponseOrdered.isCorrect(correctResponse) must equalTo(true) }
    "return false for incorrect" in { correctResponseOrdered.isCorrect(correctResponseShuffled) must equalTo(false) }
    "return false for correct with whitespace" in { correctResponseOrdered.isCorrect(correctResponseWithWhitespace) must equalTo(false) }
  }

  "isValueCorrect" should {
    val correctResponseOrdered = CorrectResponseOrdered(correctResponses)

    "return true for correct element matching index" in { correctResponseOrdered.isValueCorrect(correctResponses.head, Some(0)) must equalTo(true) }
    "return false for correct element not matching index" in { correctResponseOrdered.isValueCorrect(correctResponses.head, Some(1)) must equalTo(false) }
    "return false for correct element with whitespace matching index" in { correctResponseOrdered.isValueCorrect(s"""   $correctResponses.head   """, Some(0)) must equalTo(false) }
  }

}