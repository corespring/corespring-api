package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.models.{ User, Organization }
import org.corespring.services.OrganizationService
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import play.api.mvc.RequestHeader

import scalaz.{ Success, Failure, Validation }

class MockRequestIdentity(
  val defaultCollection: Option[ObjectId] = None,
  val org: Validation[V2Error, (Organization, Option[User])] = Failure(generalError("?")),
  val apiClientId: Option[String] = None) extends OrgRequestIdentity[String] with Mockito {

  override val name = "mock-identifier"

  override def data(rh: RequestHeader, org: Organization, apiClientId: Option[String], user: Option[User]) = Success("Worked")

  /** get the apiClient if available */
  override def headerToApiClientId(rh: RequestHeader): Option[String] = apiClientId

  override def headerToOrgAndMaybeUser(rh: RequestHeader): Validation[V2Error, (Organization, Option[User])] = org

  override def orgService: OrganizationService = {
    val o = mock[OrganizationService]
    o.findOneById(any[ObjectId]) returns org.toOption.map(_._1)
    o.defaultCollection(any[Organization]) returns defaultCollection
    o
  }

}
