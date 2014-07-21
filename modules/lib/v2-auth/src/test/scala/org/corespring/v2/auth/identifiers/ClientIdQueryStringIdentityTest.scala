package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.models.PlayerOptions
import org.corespring.v2.auth.services.OrgService
import org.corespring.v2.errors.Errors.invalidQueryStringParameter
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest

import scalaz.Failure

class ClientIdQueryStringIdentityTest extends Specification with Mockito {

  "Client id and query string identity" should {
    s"return a bad param name error" in {

      val i = new ClientIdQueryStringIdentity[String] {
        override def clientIdToOrgId(apiClientId: String): Option[ObjectId] = Some(ObjectId.get)

        override def toPlayerOptions(orgId: ObjectId, rh: RequestHeader): PlayerOptions = PlayerOptions.ANYTHING

        override def data(rh: RequestHeader, org: Organization, defaultCollection: ObjectId): String = "Worked"

        override def orgService: OrgService = {
          val m = mock[OrgService]
          m
        }
      }

      i.headerToOrgId(FakeRequest("", "?apiClientId=blah")) must_== Failure(invalidQueryStringParameter("apiClientId", ClientIdQueryStringIdentity.Keys.apiClient))
    }
  }

}
