package org.corespring.servicesAsync.auth

import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.models.Organization
import org.corespring.models.auth.AccessToken

import scalaz.Validation
import scala.concurrent.Future

trait AccessTokenService {
  def removeToken(tokenId: String): Future[Validation[PlatformServiceError, Unit]]

  def insertToken(token: AccessToken): Future[Validation[PlatformServiceError, AccessToken]]

  /**
   * Finds an access token by id
   *
   * @param tokenId - the access token id
   * @return returns an Option[AccessToken]
   */
  def findByTokenId(tokenId: String): Future[Option[AccessToken]]

  /**
   * Finds an access token by organization and scope
   *
   * @param orgId - the organization that the token was created for
   * @param scope - the scope requested when the access token was created
   * @return returns an Option[AccessToken]
   */
  def find(orgId: ObjectId, scope: Option[String]): Future[Option[AccessToken]]

  def findByOrgId(id: ObjectId): Future[Option[AccessToken]]

  /**
   * Creates an access token to invoke the APIs protected by BaseApi.
   * @return The AccessToken or ApiError if something went wrong
   *         Note: taken from legacy OAuthProvider
   */
  def createToken(clientId: String, clientSecret: String): Future[Validation[PlatformServiceError, AccessToken]]
  def getOrCreateToken(org: Organization): Future[AccessToken]

  def getOrCreateToken(orgId: ObjectId): Future[AccessToken]

  def orgForToken(token: String): Future[Validation[PlatformServiceError, Organization]]
}
