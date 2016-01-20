package org.corespring.platform.core.controllers.auth

import org.bson.types.ObjectId
import org.corespring.models.auth.{ AccessToken, ApiClient, Permission }
import org.corespring.services.auth.{ AccessTokenService, ApiClientService }
import org.corespring.services.{ OrganizationService, UserService }
import org.corespring.web.api.v1.errors.ApiError
import scalaz.Scalaz._

import scalaz.{ Failure, Success, Validation }

class OAuthProvider(
  apiClientService: ApiClientService,
  orgService: OrganizationService,
  accessTokenService: AccessTokenService,
  userService: UserService) {

  /**
   * Creates an ApiClient for an organization.  This allows organizations to receive API calls
   *
   * @param orgId - the organization id
   * @return returns an ApiClient or ApiError if the ApiClient could not be created.
   */
  def createApiClient(orgId: ObjectId): Validation[ApiError, ApiClient] = {
    apiClientService.getOrCreateForOrg(orgId).leftMap(s => ApiError(500, s))
  }

  /**
   * Creates an access token to invoke the APIs protected by BaseApi.
   *
   * @param grantType The OAuth flow (client_credentials is the only supported flow for now)
   * @param clientId The client id
   * @param clientSecret the client secret
   * @param scope If specified this must be a username.  Using the scope parameter allows the caller to ghost a user.
   * @return The AccessToken or ApiError if something went wrong
   */
  def getAccessToken(grantType: String, clientId: String, clientSecret: String, scope: Option[String] = None): Validation[ApiError, AccessToken] = {
    for {
      c <- apiClientService.findByClientIdAndSecret(clientId, clientSecret).toSuccess(ApiError(500, s"[OAuthProvider] Can't find apiClient with id: $clientId"))
      t <- accessTokenService.createToken(c).leftMap(e => ApiError(500, e.message))
    } yield t
  }

  /**
   * Gets the authorization context for an access token
   *
   * @param t The access token
   * @return Returns an Authorization Context or an ApiError if the token is invalid
   */
  def getAuthorizationContext(t: String): Validation[ApiError, AuthorizationContext] = {
    accessTokenService.findByTokenId(t) match {
      case Some(token: AccessToken) =>
        if (token.isExpired) {
          Failure(ApiError.ExpiredToken.format(token.expirationDate.toString))
        } else {
          orgService.findOneById(token.organization).map { org =>
            val permission = token.scope.flatMap { username =>
              userService.getPermissions(username, token.organization).valueOr(_ => None)
            }.getOrElse(Permission.Write)
            val context = new AuthorizationContext(token.scope, org, permission, true)
            Success(context)
          }.getOrElse {
            Failure(ApiError.InvalidToken)
          }
        }
      case _ => Failure(ApiError.InvalidToken)
    }
  }

}
