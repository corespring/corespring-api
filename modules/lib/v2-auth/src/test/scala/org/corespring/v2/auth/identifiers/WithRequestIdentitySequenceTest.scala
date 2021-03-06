package org.corespring.v2.auth.identifiers

import org.corespring.v2.errors.Errors.{ compoundError, generalError }
import org.corespring.v2.errors.V2Error
import org.specs2.execute.{ AsResult, Result }
import org.specs2.mock.Mockito
import org.specs2.mutable.{ Around, Specification }
import play.api.http.Status._
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest

import scalaz.{ Failure, Success, Validation }

class WithRequestIdentitySequenceTest extends Specification with Mockito {

  private trait scope extends Around {

    def success(name: String): RequestIdentity[String] = makeMock(name, Success(name))

    def failure(name: String) = makeMock(name, Failure(generalError(name)))

    def makeMock(name: String, v: Validation[V2Error, String]): RequestIdentity[String] = {
      val m = mock[RequestIdentity[String]]
      m.name returns name
      m.apply(any[RequestHeader]) answers { _ =>
        v
      }
      m
    }

    val fr = FakeRequest("", "")

    def identifiers: Seq[RequestIdentity[String]] = Seq.empty

    val identify = new WithRequestIdentitySequence[String] {
      override def identifiers: Seq[RequestIdentity[String]] = scope.this.identifiers
    }

    override def around[T](t: => T)(implicit evidence$1: AsResult[T]): Result = {
      AsResult.effectively(t)
    }

    lazy val result = identify(fr)
  }

  import WithRequestIdentitySequence._
  "with org transformer sequence" should {

    "fails for an empty sequence" in new scope {
      result must_== Failure(generalError(emptySequenceErrorMessage, INTERNAL_SERVER_ERROR))
    }

    "return the first success in a sequence" in new scope {
      override lazy val identifiers = Seq(failure("one"), success("one"))
      result must_== Success("one")
    }

    "return a compound error if all identifiers failed" in new scope {
      override lazy val identifiers = Seq(failure("one"), failure("two"))
      result must_== Failure(compoundError(
        errorMessage,
        Seq(generalError("one"), generalError("two")),
        UNAUTHORIZED))
    }

    "when executing" should {

      trait callScope extends scope {
        lazy val failOne = failure("one")
        lazy val failTwo = failure("two")
        lazy val successOne = success("one")
      }

      "only calls the first identifier if successful" in new callScope {
        override lazy val identifiers = Seq(successOne, failOne)
        result.isSuccess must_== true
        there was one(successOne).apply(fr)
        there was no(failOne).apply(any[RequestHeader])
      }

      "only calls each identifier once until a success has occurred" in new callScope {
        override lazy val identifiers = Seq(failOne, successOne, failTwo)
        result.isSuccess must_== true
        there was one(failOne).apply(fr)
        there was one(successOne).apply(fr)
        there was no(failTwo).apply(any[RequestHeader])
      }

      "only calls each identifier once if identification fails" in new callScope {
        override lazy val identifiers = Seq(failOne, failTwo)
        result.isFailure must_== true
        there was one(failOne).apply(fr)
        there was one(failTwo).apply(fr)
      }
    }
  }
}