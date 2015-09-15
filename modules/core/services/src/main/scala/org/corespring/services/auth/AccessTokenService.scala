package org.corespring.services.auth

import org.bson.types.ObjectId
import org.corespring.models.Organization
import org.corespring.models.auth.AccessToken
import org.corespring.services.errors.PlatformServiceError

import scalaz.Validation

trait AccessTokenService {
  def removeToken(tokenId: String): Validation[PlatformServiceError, Unit]

  def insertToken(token: AccessToken): Validation[PlatformServiceError, AccessToken]

  /**
   * Finds an access token by id
   *
   * @param tokenId - the access token id
   * @return returns an Option[AccessToken]
   */
  def findByTokenId(tokenId: String): Option[AccessToken]

  @deprecated("use findByTokenId instead", "0.1")
  def findByToken(token: String) = findByTokenId(token)

  @deprecated("use findByTokenId instead", "0.1")
  final def findById(token: String) = findByTokenId(token)

  /**
   * Finds an access token by organization and scope
   *
   * @param orgId - the organization that the token was created for
   * @param scope - the scope requested when the access token was created
   * @return returns an Option[AccessToken]
   */
  def find(orgId: ObjectId, scope: Option[String]): Option[AccessToken]

  @deprecated("use findByOrgId", "0.1")
  def getTokenForOrgById(id: ObjectId): Option[AccessToken]

  def findByOrgId(id: ObjectId): Option[AccessToken]

  /**
   * Creates an access token to invoke the APIs protected by BaseApi.
   * @return The AccessToken or ApiError if something went wrong
   *         Note: taken from legacy OAuthProvider
   */
  def createToken(clientId: String, clientSecret: String): Validation[PlatformServiceError, AccessToken]
  def getOrCreateToken(org: Organization): AccessToken

  def getOrCreateToken(orgId: ObjectId): AccessToken

  def orgForToken(token: String): Option[Organization]
}
