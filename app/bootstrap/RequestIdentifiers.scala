package bootstrap

import com.softwaremill.macwire.MacwireMacros._
import org.corespring.encryption.apiClient.ApiClientEncryptionService
import org.corespring.models.auth.ApiClient
import org.corespring.services.auth.{ AccessTokenService, ApiClientService }
import org.corespring.services.{ OrganizationService, UserService }
import org.corespring.v2.auth.identifiers._
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.errors.V2Error
import org.corespring.web.user.SecureSocial
import play.api.mvc._

import scalaz.Validation

/**
 * Identifiers that convert a [[RequestHeader]] into [[OrgAndOpts]].
 * @param secureSocialService
 * @param orgService
 * @param tokenService
 */
class RequestIdentifiers(
  secureSocialService: SecureSocial,
  orgService: OrganizationService,
  userService: UserService,
  tokenService: AccessTokenService,
  apiClientService: ApiClientService,
  apiClientEncryptionService: ApiClientEncryptionService,
  appConfig: AppConfig,
  playerTokenConfig: PlayerTokenConfig) {

  /** A token only based auth */
  def accessTokenToOrgAndApiClient: (RequestHeader) => Validation[V2Error, (OrgAndOpts, ApiClient)] =
    (r) => token.headerToOrgAndApiClient(r).map {
      case (org, apiClient) =>
        val oo = OrgAndOpts(
          org,
          PlayerAccessSettings.ANYTHING,
          AuthMode.AccessToken,
          Some(apiClient.clientId.toString),
          None)
        oo -> apiClient
    }

  lazy val userSession: UserSessionOrgIdentity = wire[UserSessionOrgIdentity]

  lazy val token: TokenOrgIdentity = wire[TokenOrgIdentity]

  lazy val clientIdAndPlayerTokenQueryString: PlayerTokenIdentity = wire[PlayerTokenIdentity]

  lazy val allIdentifiers: RequestIdentity[OrgAndOpts] = new WithRequestIdentitySequence[OrgAndOpts] {
    override def identifiers: Seq[RequestIdentity[OrgAndOpts]] = Seq(
      clientIdAndPlayerTokenQueryString,
      token,
      userSession)
  }
}

