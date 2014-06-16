package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.specs2.execute.AsResult
import org.specs2.mock.Mockito
import org.specs2.mutable.{Around, Specification}
import play.api.test.FakeRequest
import scalaz.{Failure, Success, Validation}

class WithOrgTransformerTest
  extends Specification
  with Mockito
  with TransformerSpec {

  import org.corespring.v2.auth.WithOrgTransformer._

  "With org transformer" should {

    class scope(val org: Option[Organization] = None, val defaultCollection: Option[ObjectId] = None, val orgId: Validation[String, ObjectId] = Failure("no org id")) extends Around {

      override def around[T: AsResult](t: => T) = {
        running(fakeApp) {
          AsResult.effectively(t)
        }
      }

      val tf = new MockOrgTransformer(org, defaultCollection, orgId)
    }

    /** Note: Using 'mustEqual' to prevent naming collision w/ scalaz.Validation.=== */

    "return failure - if there is no org id in header" in new scope() {
      tf(FakeRequest("", "")) mustEqual orgId
    }

    "return failure - if there is no org or default collection" in new scope(orgId = Success(ObjectId.get)) {
      tf(FakeRequest("", "")) mustEqual Failure(noOrgId(orgId.toOption.get))
    }

    "return failure - if there is no default collection" in new scope(org = Some(Organization()), orgId = Success(ObjectId.get)) {
      tf(FakeRequest("", "")) mustEqual Failure(noDefaultCollection(org.get.id))
    }

    "return some string - if there is an org + default collection" in new scope(
      Some(Organization()),
      Some(ObjectId.get),
      Success(ObjectId.get)) {
      tf(FakeRequest("", "")) mustEqual Success("Worked")
    }
  }
}
