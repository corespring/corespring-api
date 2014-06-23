package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.models.PlayerOptions
import org.corespring.v2.auth.services.OrgService
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest

import scalaz.Failure

class ClientIdAndOptionsOrgIdentityTest extends Specification with Mockito {

  val identifier = new ClientIdQueryStringIdentity[String] {
    override def clientIdToOrgId(apiClientId: String): Option[ObjectId] = None

    override def toPlayerOptions(orgId: ObjectId, rh: RequestHeader): PlayerOptions = PlayerOptions.ANYTHING

    override def data(rh: RequestHeader, org: Organization, defaultCollection: ObjectId): String = "Worked"

    override def orgService: OrgService = {
      val m = mock[OrgService]
      m
    }
  }

  "client id and options" should {
    "work" in {
      import ClientIdQueryStringIdentity.Errors
      identifier(FakeRequest("", "")) must_== Failure(Errors.noOrgForApiClient)
    }
  }
}
