package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.services.OrgService
import org.corespring.v2.errors.V2Error
import org.corespring.v2.errors.Errors.generalError
import org.specs2.mock.Mockito
import play.api.mvc.RequestHeader
import scalaz.{ Failure, Validation }

class MockRequestIdentity(
  val org: Option[Organization] = None,
  val defaultCollection: Option[ObjectId] = None,
  val orgId: Validation[V2Error, ObjectId] = Failure(generalError("?"))) extends OrgRequestIdentity[String] with Mockito {

  override def data(rh: RequestHeader, org: Organization, defaultCollection: ObjectId): String = "Worked"

  override def headerToOrgId(rh: RequestHeader): Validation[V2Error, ObjectId] = orgId

  override def orgService: OrgService = {
    val o = mock[OrgService]
    o.org(any[ObjectId]) returns org
    o.defaultCollection(any[Organization]) returns defaultCollection
    o
  }

}
