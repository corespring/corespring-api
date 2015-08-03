package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.models.Organization
import org.corespring.v2.errors.Errors.generalError
import org.specs2.execute.{ AsResult, Result }
import org.specs2.mock.Mockito
import org.specs2.mutable.{ Around, Specification }
import play.api.test.FakeRequest

import scalaz.{ Success, Failure }

class WithRequestIdentitySequenceTest extends Specification with Mockito {

  class scope(tfs: Seq[OrgRequestIdentity[String]]) extends Around {

    val seq = new WithRequestIdentitySequence[String] {
      override def identifiers: Seq[OrgRequestIdentity[String]] = tfs
    }

    override def around[T](t: => T)(implicit evidence$1: AsResult[T]): Result = {
      AsResult.effectively(t)
    }
  }

  def successfulTransformer = new MockRequestIdentity(Some(ObjectId.get), Success((mock[Organization], None)))

  def failedTransformer = new MockRequestIdentity()

  "with org transformer sequence" should {

    "fails for an empty sequence" in new scope(Seq.empty) {

      import play.api.http.Status._
      seq(FakeRequest("", "")) must_== Failure(generalError(WithRequestIdentitySequence.emptySequenceErrorMessage, INTERNAL_SERVER_ERROR))
    }

    "return the first success in a sequence" in new scope(Seq(
      failedTransformer,
      successfulTransformer)) {
      seq(FakeRequest("", "")) mustEqual Success("Worked")
    }
  }
}
