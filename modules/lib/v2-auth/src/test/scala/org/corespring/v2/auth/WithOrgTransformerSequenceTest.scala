package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.{Around, Specification}
import play.api.test.FakeRequest

import scalaz.{Success, Failure}

class WithOrgTransformerSequenceTest extends Specification with TransformerSpec{

  class scope(tfs:Seq[WithServiceOrgTransformer[String]]) extends Around{

    val seq = new WithOrgTransformerSequence[String] {
      override def transformers: Seq[WithServiceOrgTransformer[String]] = tfs
    }

    override def around[T](t: => T)(implicit evidence$1: AsResult[T]): Result = {
      running(fakeApp){
        AsResult.effectively(t)
      }
    }
  }

  def successfulTransformer = new MockOrgTransformer(Some(Organization()), Some(ObjectId.get), Success(ObjectId.get))
  def failedTransformer = new MockOrgTransformer()

  "with org transformer sequence" should {

    "fails for an empty sequence" in new scope(Seq.empty){
      seq(FakeRequest("","")) mustEqual Failure("Failed to transform request")
    }

    "return the first success in a sequence" in new scope(Seq(
      failedTransformer,
      successfulTransformer
    )){
      seq(FakeRequest("","")) mustEqual Success("Worked")
    }
  }
}
