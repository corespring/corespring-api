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
      val org: Option[Organization] = None,
      val defaultCollection: Option[ObjectId] = None,
      val orgId: Validation[V2Error, ObjectId] = Failure(generalError("?"))) extends Around {

      override def around[T: AsResult](t: => T) = {
        running(fakeApp) {
          AsResult.effectively(t)
        }
      }

      val tf = new MockRequestIdentity(org, defaultCollection, orgId)
    }

    /** Note: Using 'mustEqual' to prevent naming collision w/ scalaz.Validation.=== */

    "return failure - if there is no org id in header" in new scope() {
      tf(FakeRequest("", "")) mustEqual orgId
    }

    "return failure - if there is no org or default collection" in new scope(orgId = Success(ObjectId.get)) {
      tf(FakeRequest("", "")) mustEqual Failure(cantFindOrgWithId(orgId.toOption.get))
    }

    "return failure - if there is no default collection" in new scope(org = Some(mock[Organization]), orgId = Success(ObjectId.get)) {
      tf(FakeRequest("", "")) mustEqual Failure(noDefaultCollection(org.get.id))
    }

    "return some string - if there is an org + default collection" in new scope(
      Some(mock[Organization]),
      Some(ObjectId.get),
      Success(ObjectId.get)) {
      tf(FakeRequest("", "")) mustEqual Success("Worked")
    }
  }
}
