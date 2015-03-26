package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.services.OrgService
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import play.api.mvc.RequestHeader

import scalaz.{ Success, Failure, Validation }

class MockRequestIdentity(
  val defaultCollection: Option[ObjectId] = None,
  val org: Validation[V2Error, Organization] = Failure(generalError("?")),
  val apiClientId: Option[String] = None) extends OrgRequestIdentity[String] with Mockito {

  override def data(rh: RequestHeader, org: Organization, apiClientId: Option[String]) = Success("Worked")

  /** get the apiClient if available */
  override def headerToApiClientId(rh: RequestHeader): Option[String] = apiClientId

  override def headerToOrgAndMaybeUser(rh: RequestHeader): Validation[V2Error, Organization] = org

  override def orgService: OrgService = {
    val o = mock[OrgService]
    o.org(any[ObjectId]) returns org.toOption
    o.defaultCollection(any[Organization]) returns defaultCollection
    o
  }

}
