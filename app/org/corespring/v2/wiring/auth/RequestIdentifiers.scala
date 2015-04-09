package org.corespring.v2.wiring.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.controllers.auth.SecureSocialService
import org.corespring.platform.core.encryption.OrgEncryptionService
import org.corespring.platform.core.models.{ User, Organization }
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.platform.core.services._
import org.corespring.v2.auth.identifiers._
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.auth.services.{ OrgService, TokenService }
import play.api.mvc._
import play.api.{ Play, Mode => PlayMode }

import scalaz.Success

/**
 * Identifiers that convert a <RequestHeader> into <OrgAndOpts>.
 * @param secureSocialService
 * @param orgService
 * @param tokenService
 */
class RequestIdentifiers(
  secureSocialService: SecureSocialService,
  orgService: OrgService,
  tokenService: TokenService,
  orgEncryptionService: OrgEncryptionService,
  isDevToolsEnabled: Boolean = false) {

  lazy val userSession = new UserSessionOrgIdentity[OrgAndOpts] {
    override def secureSocialService: SecureSocialService = RequestIdentifiers.this.secureSocialService

    override def userService: UserService = UserServiceWired

    override def data(rh: RequestHeader, org: Organization, apiClientId: Option[String], user: Option[User]) = Success(
      OrgAndOpts(org, PlayerAccessSettings.ANYTHING, AuthMode.UserSession, apiClientId, user))

    override def orgService: OrgService = RequestIdentifiers.this.orgService
  }

  lazy val token = new TokenOrgIdentity[OrgAndOpts] {
    override def tokenService: TokenService = RequestIdentifiers.this.tokenService

    override def data(rh: RequestHeader, org: Organization, apiClientId: Option[String], user: Option[User]) = Success(
      OrgAndOpts(org, PlayerAccessSettings.ANYTHING, AuthMode.AccessToken, apiClientId))

    override def orgService: OrgService = RequestIdentifiers.this.orgService
  }

  lazy val clientIdAndPlayerTokenQueryString = new PlayerTokenInQueryStringIdentity {

    override def orgService: OrgService = RequestIdentifiers.this.orgService

    /** for a given apiClient return the org Id */
    override def clientIdToOrg(apiClientId: String): Option[Organization] = {
      logger.trace(s"client to orgId -> $apiClientId")
      for {
        client <- ApiClient.findByKey(apiClientId)
        org <- orgService.org(client.orgId)
      } yield org
    }

    private def encryptionEnabled(r: RequestHeader): Boolean = {
      val m = Play.current.mode
      val acceptsFlag = m == PlayMode.Dev || m == PlayMode.Test || isDevToolsEnabled

      val enabled = if (acceptsFlag) {
        val disable = r.getQueryString("skipDecryption").map(v => true).getOrElse(false)
        !disable
      } else true
      enabled
    }

    override def decrypt(encrypted: String, orgId: ObjectId, header: RequestHeader): Option[String] =
      if (!encryptionEnabled(header)) {
        Some(encrypted)
      } else {
        orgEncryptionService.decrypt(orgId, encrypted)
      }
  }

  lazy val allIdentifiers: RequestIdentity[OrgAndOpts] = new WithRequestIdentitySequence[OrgAndOpts] {
    override def identifiers: Seq[OrgRequestIdentity[OrgAndOpts]] = Seq(
      clientIdAndPlayerTokenQueryString,
      token,
      userSession)
  }
}
