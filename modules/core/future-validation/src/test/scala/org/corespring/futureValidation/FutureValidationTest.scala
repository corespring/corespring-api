package org.corespring.futureValidation

import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext
import scalaz.{ Failure, Success }

class FutureValidationTest extends Specification {

  import ExecutionContext.Implicits.global

  val suc = Success("success")
  val fai = Failure("failure")

  def gotWhat(v: String): FutureValidation[String, String] = {
    FutureValidation(Success(s"success: $v"))
  }

  def gotWhatMap(v: String): String = {
    s"success: $v"
  }

  def gotWhatLeftMap(v: String): String = {
    s"failure: $v"
  }

  "flatMap" should {
    "return a FutureValidation[EE,B] Success" in {
      FutureValidation(suc).flatMap(gotWhat).future must equalTo(Success("success: success")).await
    }

    "return a FutureValidation[EE,B] Failure" in {
      FutureValidation(fai).flatMap(gotWhat).future must equalTo(fai).await
    }
  }

  "map" should {
    "return a FutureValidation[E,B] Success" in {
      FutureValidation(suc).map(gotWhatMap).future must equalTo(Success("success: success")).await
    }

    "return a FutureValidation[E,B] Failure" in {
      FutureValidation(fai).map(gotWhatMap).future must equalTo(fai).await
    }
  }

  "leftMap" should {
    "return a FutureValidation[EE,B] Success" in {
      FutureValidation(suc).leftMap(gotWhatLeftMap).future must equalTo(suc).await
    }

    "return a FutureValidation[EE,B] Failure" in {
      FutureValidation(fai).leftMap(gotWhatLeftMap).future must equalTo(Failure(gotWhatLeftMap("failure"))).await
    }
  }
}
