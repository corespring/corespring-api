package bootstrap

import org.corespring.encryption.apiClient.ApiClientEncryptionService
import org.corespring.models.{ User, Organization }
import org.corespring.services.{ UserService, OrganizationService }
import org.corespring.services.auth.{ ApiClientService, AccessTokenService }
import org.corespring.v2.auth.identifiers.OrgRequestIdentity
import org.corespring.v2.auth.identifiers.PlayerTokenInQueryStringIdentity
import org.corespring.v2.auth.identifiers.RequestIdentity
import org.corespring.v2.auth.identifiers.TokenOrgIdentity
import org.corespring.v2.auth.identifiers.UserSessionOrgIdentity
import org.corespring.v2.auth.identifiers.WithRequestIdentitySequence
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts, PlayerAccessSettings }
import org.corespring.web.user.SecureSocial
import play.api.mvc._
import play.api.{ Play, Mode => PlayMode }

import scalaz.Success

/**
 * Identifiers that convert a <RequestHeader> into <OrgAndOpts>.
 * @param secureSocialService
 * @param orgService
 * @param tokenService
 */
class RequestIdentifiers(secureSocialService: SecureSocial,
  orgService: OrganizationService,
  userService: UserService,
  tokenService: AccessTokenService,
  apiClientService: ApiClientService,
  apiClientEncryptionService: ApiClientEncryptionService) {

  lazy val userSession = new UserSessionOrgIdentity[OrgAndOpts] {
    override def secureSocial: SecureSocial = RequestIdentifiers.this.secureSocialService

    override def data(rh: RequestHeader, org: Organization, apiClientId: Option[String], user: Option[User]) = Success(
      OrgAndOpts(org, PlayerAccessSettings.ANYTHING, AuthMode.UserSession, apiClientId, user))

    override def orgService: OrganizationService = RequestIdentifiers.this.orgService

    override def userService: UserService = RequestIdentifiers.this.userService
  }

  lazy val token = new TokenOrgIdentity[OrgAndOpts](
    tokenService,
    orgService) {
    override def data(rh: RequestHeader, org: Organization, apiClientId: Option[String], user: Option[User]) = Success(
      OrgAndOpts(org, PlayerAccessSettings.ANYTHING, AuthMode.AccessToken, apiClientId))

  }

  lazy val clientIdAndPlayerTokenQueryString = new PlayerTokenInQueryStringIdentity {

    override def orgService: OrganizationService = RequestIdentifiers.this.orgService

    /** for a given apiClient return the org Id */
    override def clientIdToOrg(apiClientId: String): Option[Organization] = {
      logger.trace(s"client to orgId -> $apiClientId")
      for {
        client <- apiClientService.findByClientId(apiClientId)
        org <- orgService.findOneById(client.orgId)
      } yield org
    }

    private def encryptionEnabled(r: RequestHeader): Boolean = {
      val m = Play.current.mode
      val acceptsFlag = m == PlayMode.Dev || m == PlayMode.Test

      val enabled = if (acceptsFlag) {
        val disable = r.getQueryString("skipDecryption").map(v => true).getOrElse(false)
        !disable
      } else true
      enabled
    }

    override def decrypt(encrypted: String, apiClient: String, header: RequestHeader): Option[String] =
      if (!encryptionEnabled(header)) {
        Some(encrypted)
      } else {
        apiClientEncryptionService.decrypt(apiClient, encrypted)
      }
  }

  lazy val allIdentifiers: RequestIdentity[OrgAndOpts] = new WithRequestIdentitySequence[OrgAndOpts] {
    override def identifiers: Seq[OrgRequestIdentity[OrgAndOpts]] = Seq(
      clientIdAndPlayerTokenQueryString,
      token,
      userSession)
  }
}

