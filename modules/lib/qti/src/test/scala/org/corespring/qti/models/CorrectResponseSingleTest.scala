package org.corespring.qti.models

import org.specs2.mutable.Specification

class CorrectResponseSingleTest extends Specification {

  val correctResponse = "A"
  val correctResponseWithWhitespace = s"""   $correctResponse   """
  val incorrectResponse = "B"

  "isCorrect" should {
    val correctResponseSingle = CorrectResponseSingle(correctResponse)

    "return true for correct" in { correctResponseSingle.isCorrect(correctResponse) must equalTo(true) }
    "return false for correct with whitespace" in { correctResponseSingle.isCorrect(correctResponseWithWhitespace) must equalTo(false) }
    "return false for incorrect" in  { correctResponseSingle.isCorrect(incorrectResponse) must equalTo(false) }
  }

  "isValueCorrect" should {
    val correctResponseSingle = CorrectResponseSingle(correctResponse)

    "return true for correct for any index" in {
      correctResponseSingle.isValueCorrect(correctResponse, Some(0)) must equalTo(true)
      correctResponseSingle.isValueCorrect(correctResponse, Some(10)) must equalTo(true)
    }

    "return false for correct with whitespace for any index" in {
      correctResponseSingle.isValueCorrect(correctResponseWithWhitespace, Some(0)) must equalTo(false)
      correctResponseSingle.isValueCorrect(correctResponseWithWhitespace, Some(10)) must equalTo(false)
    }

    "return false for incorrect for any index" in {
      correctResponseSingle.isValueCorrect(incorrectResponse, Some(0)) must equalTo(false)
      correctResponseSingle.isValueCorrect(incorrectResponse, Some(10)) must equalTo(false)
    }
  }

}
