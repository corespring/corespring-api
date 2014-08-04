package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.services.OrgService
import org.corespring.v2.errors.Errors.{ cantFindOrgWithId, noDefaultCollection }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.mvc.RequestHeader

import scalaz.Validation

/**
 * Turn an unknown request header into an identity so decisions can be made about the request.
 * @tparam B the identity type
 */
trait RequestIdentity[B] {
  def apply(rh: RequestHeader): Validation[V2Error, B]
}

trait OrgRequestIdentity[B] extends RequestIdentity[B] {
  def orgService: OrgService

  def headerToOrgId(rh: RequestHeader): Validation[V2Error, ObjectId]

  def data(rh: RequestHeader, org: Organization, defaultCollection: ObjectId): B

  lazy val logger = V2LoggerFactory.getLogger("auth", "OrgRequestIdentity")

  def apply(rh: RequestHeader): Validation[V2Error, B] = {

    logger.trace(s"apply: ${rh.path}")
    import scalaz.Scalaz._

    for {
      orgId <- headerToOrgId(rh)
      org <- orgService.org(orgId).toSuccess(cantFindOrgWithId(orgId))
      dc <- orgService.defaultCollection(org).toSuccess(noDefaultCollection(org.id))
    } yield {
      data(rh, org, dc)
    }
  }
}

