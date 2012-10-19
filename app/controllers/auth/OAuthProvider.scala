package controllers.auth

import models.auth.{AccessToken, ApiClient}
import api.ApiError._
import api.ApiError
import org.bson.types.ObjectId
import models.Organization
import play.api.Logger
import com.novus.salat.dao.SalatSaveError
import org.joda.time.DateTime

/**
 * A OAuth provider
 */
object OAuthProvider {

  /**
   * Creates an ApiClient for an organization.  This allows organizations to receive API calls
   *
   * @param orgId - the organization id
   * @return returns an ApiClient or ApiError if the ApiClient could not be created.
   */
  def register(orgId: ObjectId): Either[ApiError, ApiClient] = {
    // check we got an existing org id
    Organization.findOneById(orgId) match {
      case Some(org) =>
        val apiClient = ApiClient(orgId, new ObjectId(), generateToken)
        try {
          ApiClient.save(apiClient)
          Right(apiClient)
        } catch {
          case e: SalatSaveError => {
            Logger.error("Error registering ortganization %s".format(orgId), e)
            Left(ApiError.OperationError)
          }
        }
      case None => Left(UnknownOrganization)
    }
  }

  def register(orgId:ObjectId, userId:ObjectId, pass:String): Either[ApiError, ApiClient] = {
    // check we got an existing org id
    Organization.findOneById(orgId) match {
      case Some(org) =>
        val apiClient = ApiClient(orgId, userId, pass)
        try {
          ApiClient.save(apiClient)
          Right(apiClient)
        } catch {
          case e: SalatSaveError => {
            Logger.error("Error registering ortganization %s".format(orgId), e)
            Left(ApiError.OperationError)
          }
        }
      case None => Left(UnknownOrganization)
    }
  }

  /**
   * Creates an access token to invoke the APIs protected by BaseApi.
   *
   * @param grantType The OAuth flow (client_credentials is the only supported flow for now)
   * @param clientId The client id
   * @param clientSecret The client secret
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
            val org = client.id
            AccessToken.find(org, scope).foreach(AccessToken.remove(_))
            val creationTime = DateTime.now()
            val token = AccessToken(org, scope, generateToken, creationTime, creationTime.plusHours(24))
            AccessToken.insert(token) match {
              case Some(_) => Right(token)
              case None => Left(ApiError.OperationError)
            }
        }).getOrElse(Left(InvalidCredentials))
      case _ => Left(UnsupportedFlow)
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
        if ( token.isExpired ) {
          Left(ExpiredToken.format(token.expirationDate.toString))
        } else {
          Right(new AuthorizationContext(token.organization, token.scope))
        }
      case _ => Left(InvalidToken)
    }
  }

  /**
   * Generates a random token
   *
   * @return a token
   */
  def generateToken = {
    BigInt.probablePrime(100, scala.util.Random).toString(36)
  }
}
