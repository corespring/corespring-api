package org.corespring.v2.auth.identifiers

import org.corespring.models.{ User, Organization }
import org.corespring.services.OrganizationService
import org.corespring.v2.errors.V2Error
import play.api.Logger
import play.api.mvc.RequestHeader

import scalaz.Validation

/**
 * Turn an unknown request header into an identity so decisions can be made about the request.
 * @tparam B the identity type
 */
trait RequestIdentity[B] {
  def name: String
  def apply(rh: RequestHeader): Validation[V2Error, B]
}

trait OrgRequestIdentity[B] extends RequestIdentity[B] {
  def orgService: OrganizationService

  /** get either a V2Error or the org from the request header */
  def headerToOrgAndMaybeUser(rh: RequestHeader): Validation[V2Error, (Organization, Option[User])]

  /** get the apiClient if available */
  def headerToApiClientId(rh: RequestHeader): Option[String]

  /** convert the header, org and defaultCollection into the expected output type B */
  def data(rh: RequestHeader, org: Organization, apiClientId: Option[String], user: Option[User]): Validation[V2Error, B]

  lazy val logger = Logger(classOf[OrgRequestIdentity[B]])

  def apply(rh: RequestHeader): Validation[V2Error, B] = {

    logger.trace(s"apply: ${rh.path}")

    for {
      orgAndUser <- headerToOrgAndMaybeUser(rh)
      result <- data(rh, orgAndUser._1, headerToApiClientId(rh), orgAndUser._2)
    } yield {
      result
    }
  }

}

