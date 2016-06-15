package org.corespring.services.auth

import org.corespring.errors.PlatformServiceError
import org.corespring.models.Organization
import org.corespring.models.auth.{ AccessToken, ApiClient }

import scalaz.Validation

trait UpdateAccessTokenService {
  def update(token: AccessToken): Validation[PlatformServiceError, AccessToken]
}

trait AccessTokenService {
  def removeToken(tokenId: String): Validation[PlatformServiceError, Unit]

  /**
   * Finds an access token by id
   *
   * @param tokenId - the access token id
   * @return returns an Option[AccessToken]
   */
  def findByTokenId(tokenId: String): Option[AccessToken]

  /**
   * Creates an access token to invoke the APIs protected by BaseApi.
   * @return The AccessToken or ApiError if something went wrong
   *         Note: taken from legacy OAuthProvider
   */
  def createToken(apClient: ApiClient): Validation[PlatformServiceError, AccessToken]

  def orgForToken(token: String): Validation[PlatformServiceError, Organization]
}
