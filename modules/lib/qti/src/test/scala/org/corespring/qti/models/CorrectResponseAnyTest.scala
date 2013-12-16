package org.corespring.qti.models

import org.specs2.mutable.Specification

class CorrectResponseAnyTest extends Specification {


  val correctResponse = "A"
  val anotherCorrectResponse = "B"
  val incorrectResponse = "C"
  val correctResponseWithWhitespace = s"""   $correctResponse   """

  val correctResponses = Seq(correctResponse, anotherCorrectResponse)


  "isCorrect" should {
    val correctResponseAny = CorrectResponseAny(correctResponses)

    "return true for correct" in {
      correctResponseAny.isCorrect(correctResponse) must equalTo(true)
      correctResponseAny.isCorrect(anotherCorrectResponse) must equalTo(true)
    }

    "return false for incorrect" in { correctResponseAny.isCorrect(incorrectResponse) must equalTo(false) }
    "return false for correct with whitepsace" in { correctResponseAny.isCorrect(correctResponseWithWhitespace) must equalTo(false) }
  }

  "isValueCorrect" should {
    val correctResponseAny = CorrectResponseAny(correctResponses)

    "return true for correct for any index" in {
      correctResponseAny.isValueCorrect(correctResponse, Some(0)) must equalTo(true)
      correctResponseAny.isValueCorrect(correctResponse, Some(10)) must equalTo(true)
    }

    "return false for correct with whitespace for any index" in {
      correctResponseAny.isValueCorrect(correctResponseWithWhitespace, Some(0)) must equalTo(false)
      correctResponseAny.isValueCorrect(correctResponseWithWhitespace, Some(10)) must equalTo(false)
    }

    "return false for incorrect for any index" in {
      correctResponseAny.isValueCorrect(incorrectResponse, Some(0)) must equalTo(false)
      correctResponseAny.isValueCorrect(incorrectResponse, Some(10)) must equalTo(false)
    }

  }

}