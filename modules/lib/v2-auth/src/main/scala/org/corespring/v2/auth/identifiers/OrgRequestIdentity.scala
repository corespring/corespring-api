package org.corespring.v2.auth.identifiers

import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.services.OrgService
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

  /** get either a V2Error or the org from the request header */
  def headerToOrg(rh: RequestHeader): Validation[V2Error, Organization]

  /** get the apiClient if available */
  def headerToApiClientId(rh: RequestHeader): Option[String]

  /** convert the header, org and defaultCollection into the expected output type B */
  def data(rh: RequestHeader, org: Organization, apiClientId: Option[String]): Validation[V2Error, B]

  lazy val logger = V2LoggerFactory.getLogger("auth", "OrgRequestIdentity")

  def apply(rh: RequestHeader): Validation[V2Error, B] = {

    logger.trace(s"apply: ${rh.path}")

    for {
      org <- headerToOrg(rh)
      result <- data(rh, org, headerToApiClientId(rh))
    } yield {
      result
    }
  }
}

