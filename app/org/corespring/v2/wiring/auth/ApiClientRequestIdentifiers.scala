package org.corespring.v2.wiring.auth

import org.corespring.platform.core.controllers.auth.SecureSocialService
import org.corespring.platform.core.encryption.OrgEncryptionService
import org.corespring.platform.core.models.auth.{ApiClientService, ApiClient}
import org.corespring.platform.core.services._
import org.corespring.v2.auth.identifiers._
import org.corespring.v2.auth.services.{ OrgService, TokenService }
import play.api.mvc._

class ApiClientRequestIdentifiers(
  secureSocialService: SecureSocialService,
  apiClientService: ApiClientService,
  orgService: OrgService,
  tokenService: TokenService,
  orgEncryptionService: OrgEncryptionService,
  isDevToolsEnabled: Boolean = false) {

  lazy val userSession = new UserSessionApiClientIdentity[ApiClient] {
    override def secureSocialService: SecureSocialService = ApiClientRequestIdentifiers.this.secureSocialService
    override def userService: UserService = UserServiceWired
    override def apiClientService: ApiClientService = ApiClientRequestIdentifiers.this.apiClientService
    override def data(rh: RequestHeader, apiClient: ApiClient): ApiClient =
      headerToApiClient(rh).getOrElse(throw new Exception("wahtever"))
  }

  lazy val token = new TokenApiClientIdentity[ApiClient] {
    override def tokenService: TokenService = ApiClientRequestIdentifiers.this.tokenService
    override def apiClientService: ApiClientService = ApiClientRequestIdentifiers.this.apiClientService
    override def data(rh: RequestHeader, apiClient: ApiClient): ApiClient = headerToApiClient(rh).getOrElse(throw new Exception("wahtever"))
  }

  lazy val clientIdAndPlayerTokenQueryString = new ApiClientPlayerTokenInQueryStringIdentity {
    override def clientIdToApiClient(apiClientId: String) = apiClientService.findByKey(apiClientId)
    override def data(rh: RequestHeader, apiClient: ApiClient) = headerToApiClient(rh).getOrElse(throw new Exception("wahtever"))
    override def apiClientService: ApiClientService = ApiClientRequestIdentifiers.this.apiClientService
  }

  lazy val allIdentifiers: RequestIdentity[ApiClient] = new WithRequestIdentitySequence[ApiClient] {
    override def identifiers: Seq[RequestIdentity[ApiClient]] = Seq(
      clientIdAndPlayerTokenQueryString,
      token,
      userSession)
  }
}
