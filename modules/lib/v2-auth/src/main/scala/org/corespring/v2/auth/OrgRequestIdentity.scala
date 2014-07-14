package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.services.OrgService
import org.slf4j.{ Logger, LoggerFactory }
import play.api.mvc.RequestHeader

import scalaz.Validation

trait RequestIdentity[B] {
  /**
   * Turn an unknown request header into an identity so decisions can be made about the request.
   * @param rh
   * @return
   */
  def apply(rh: RequestHeader): Validation[String, B]
}

object OrgRequestIdentity {
  def noOrgId(orgId: ObjectId) = s"No org for orgId $orgId"
  def noDefaultCollection(orgId: ObjectId) = s"No default collection for org ${orgId}"
}

trait HeaderAsOrgId {
  def headerToOrgId(rh: RequestHeader): Validation[String, ObjectId]
}

trait OrgRequestIdentity[B] extends RequestIdentity[B] with HeaderAsOrgId {
  def orgService: OrgService

  def data(rh: RequestHeader, org: Organization, defaultCollection: ObjectId): B

  lazy val logger: Logger = LoggerFactory.getLogger("v2.auth.WithOrgTransformer")

  def apply(rh: RequestHeader): Validation[String, B] = {
    import org.corespring.v2.auth.OrgRequestIdentity._

    import scalaz.Scalaz._

    for {
      orgId <- headerToOrgId(rh)
      org <- orgService.org(orgId).toSuccess(noOrgId(orgId))
      dc <- orgService.defaultCollection(org).toSuccess(noDefaultCollection(org.id))
    } yield {
      data(rh, org, dc)
    }
  }
}

