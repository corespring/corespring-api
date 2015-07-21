package org.corespring.services.auth

import org.bson.types.ObjectId
import org.corespring.models.Organization
import org.corespring.models.auth.AccessToken
import org.corespring.services.errors.PlatformServiceError

trait AccessTokenService {
  def removeToken(tokenId: String): Either[PlatformServiceError, Unit]

  def insertToken(token: AccessToken): Either[PlatformServiceError, AccessToken]

  /**
   * Finds an access token by id
   *
   * @param tokenId - the access token id
   * @return returns an Option[AccessToken]
   */
  def findByTokenId(tokenId: String): Option[AccessToken]

  @deprecated("use findByTokenId instead", "0.1")
  def findByToken(token: String) = findByTokenId(token)

  final def findById(token: String) = throw new NotImplementedError("Use 'findByTokenId' instead")
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

  def getOrCreateToken(org: Organization): AccessToken
}
