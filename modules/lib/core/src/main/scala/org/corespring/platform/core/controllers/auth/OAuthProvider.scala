package org.corespring.platform.core.controllers.auth

import com.novus.salat.dao.SalatSaveError
import org.bson.types.ObjectId
import org.corespring.api.v1.errors.ApiError
import org.corespring.common.encryption.AESCrypto
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.{ ApiClient, AccessToken }
import org.joda.time.DateTime
import play.api.Logger
import scala.Some

trait AuthTokenGenerating{
  /**
   * Generates a random token
   *
   * @return a token
   */
  def generateToken(keyLength : Int = AESCrypto.KEY_LENGTH) = {
    BigInt.probablePrime(keyLength * 8, scala.util.Random).toString(AESCrypto.KEY_RADIX)
  }
}

/**
 * A OAuth provider
 */
object OAuthProvider extends AuthTokenGenerating{

  /**
   * Creates an ApiClient for an organization.  This allows organizations to receive API calls
   *
   * @param orgId - the organization id
   * @return returns an ApiClient or ApiError if the ApiClient could not be created.
   */
  def createApiClient(orgId: ObjectId): Either[ApiError, ApiClient] = {
    ApiClient.findOneByOrgId(orgId) match {
      case Some(apiClient) => Right(apiClient)
      case None => {
        // check we got an existing org id
        Organization.findOneById(orgId) match {
          case Some(org) =>
            val apiClient = ApiClient(orgId, new ObjectId(), generateToken())
            try {
              ApiClient.save(apiClient)
              Right(apiClient)
            } catch {
              case e: SalatSaveError => {
                Logger.error("Error registering ortganization %s".format(orgId), e)
                Left(ApiError.OperationError)
              }
            }
          case None => Left(ApiError.UnknownOrganization)
        }
      }
    }
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
  def getAccessToken(grantType: String, clientId: String, clientSecret: String, scope: Option[String] = None): Either[ApiError, AccessToken] = {
    grantType match {
      case OAuthConstants.ClientCredentials =>
        // check we got valid credentials first
        ApiClient.findByIdAndSecret(clientId, clientSecret).map(
          {
            client =>
              //todo: if a user if specified check that it exists and is visible for the caller

              // credentials are ok, delete if there's a previous token for the same org and scope
              val org = client.orgId
              AccessToken.find(org, scope).foreach(AccessToken.remove(_))
              val creationTime = DateTime.now()
              val token = AccessToken(org, scope, generateToken(), creationTime, creationTime.plusHours(24))
              AccessToken.insert(token) match {
                case Some(_) => Right(token)
                case None => Left(ApiError.OperationError)
              }
          }).getOrElse(Left(ApiError.InvalidCredentials))
      case _ => Left(ApiError.UnsupportedFlow)
    }
  }

  /**
   * Gets the authorization context for an access token
   *
   * @param t The access token
   * @return Returns an Authorization Context or an ApiError if the token is invalid
   */
  def getAuthorizationContext(t: String): Either[ApiError, AuthorizationContext] = {
    AccessToken.findById(t) match {
      case Some(token: AccessToken) =>
        if (token.isExpired) {
          Left(ApiError.ExpiredToken.format(token.expirationDate.toString))
        } else {

          Right(new AuthorizationContext(token.organization, token.scope, false, Organization.findOneById(token.organization)))
        }
      case _ => Left(ApiError.InvalidToken)
    }
  }

}
