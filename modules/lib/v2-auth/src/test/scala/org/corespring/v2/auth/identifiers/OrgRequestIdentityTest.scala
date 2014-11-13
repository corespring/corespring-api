package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.v2.errors.Errors.{ generalError, cantFindOrgWithId, noDefaultCollection }
import org.corespring.v2.errors.V2Error
import org.specs2.execute.AsResult
import org.specs2.mock.Mockito
import org.specs2.mutable.{ Around, Specification }
import play.api.test.FakeRequest

import scalaz.{ Failure, Success, Validation }

class OrgRequestIdentityTest
  extends Specification
  with Mockito
  with IdentitySpec {

  "With org transformer" should {

    class scope(
      val defaultCollection: Option[ObjectId] = None,
      val org: Validation[V2Error, Organization] = Failure(generalError("?"))) extends Around {

      override def around[T: AsResult](t: => T) = {
        running(fakeApp) {
          AsResult.effectively(t)
        }
      }

      val tf = new MockRequestIdentity(defaultCollection, org)
    }

    /** Note: Using 'mustEqual' to prevent naming collision w/ scalaz.Validation.=== */

    "return failure - if there is no org id in header" in new scope() {
      tf(FakeRequest("", "")) mustEqual org
    }

    "return failure - if there is no default collection" in new scope(org = Success(mock[Organization])) {
      tf(FakeRequest("", "")) mustEqual Failure(noDefaultCollection(org.toEither.right.get.id))
    }

    "return some string - if there is an org + default collection" in new scope(
      Some(ObjectId.get),
      Success(mock[Organization])) {
      tf(FakeRequest("", "")) mustEqual Success("Worked")
    }
  }
}
