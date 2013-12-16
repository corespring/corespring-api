package org.corespring.qti.models

import org.specs2.mutable.Specification

class CorrectResponseMultipleTest extends Specification {

  val correctResponses = Seq("2x+y", "y+2x")
  val correctElement = correctResponses.head
  val incorrectElement = "3x+y"
  val correctElementWithWhitespace = s"""    $correctElement   """
  val correctResponse = correctResponses.mkString(",")
  val correctResponseWithWhitespace = correctResponses.map(_.toCharArray.mkString(" ")).mkString(" , ")
  val incorrectResponse = Seq(correctElement, "x+y").mkString(",")


  "isCorrect" should {
    val correctResponseMultiple = CorrectResponseMultiple(correctResponses)

    "return true for correct" in { correctResponseMultiple.isCorrect(correctResponse) must equalTo(true) }
    "return false for correct with whitespace" in { correctResponseMultiple.isCorrect(correctResponseWithWhitespace) must equalTo(false) }
    "return false for incorrect" in { correctResponseMultiple.isCorrect(incorrectResponse) must equalTo(false) }
  }

  "isPartOfCorrect" should {
    val correctResponseMultiple = CorrectResponseMultiple(correctResponses)

    "return true for correct" in { correctResponseMultiple.isPartOfCorrect(correctElement) must equalTo(true) }
    "return false for correct with whitespace" in { correctResponseMultiple.isPartOfCorrect(correctElementWithWhitespace) must equalTo(false) }
    "return false for incorrect" in { correctResponseMultiple.isPartOfCorrect(incorrectElement) must equalTo(false) }
  }

  "isValueCorrect" should {
    val correctResponseMultiple = CorrectResponseMultiple(correctResponses)

    "return true for correct element for any index" in {
      correctResponseMultiple.isValueCorrect(correctElement, Some(0)) must equalTo(true)
      correctResponseMultiple.isValueCorrect(correctElement, Some(10)) must equalTo(true)
    }

    "return false for correct element with whitespace for any index" in {
      correctResponseMultiple.isValueCorrect(correctElementWithWhitespace, Some(0)) must equalTo(false)
      correctResponseMultiple.isValueCorrect(correctElementWithWhitespace, Some(10)) must equalTo(false)
    }

    "return false for incorrect element for any index" in {
      correctResponseMultiple.isValueCorrect(incorrectElement, Some(0)) must equalTo(false)
      correctResponseMultiple.isValueCorrect(incorrectElement, Some(10)) must equalTo(false)
    }
  }

}
