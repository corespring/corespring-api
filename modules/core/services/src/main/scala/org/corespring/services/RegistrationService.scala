package org.corespring.services

import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.models.Organization
import org.corespring.models.auth.ApiClient
import org.corespring.models.registration.RegistrationToken

case class RegistrationInfo(org: Organization, defaultCollection: ObjectId, apiClient: ApiClient)

trait RegistrationTokenService {

  def createToken(token: RegistrationToken): Boolean
  def findTokenByUuid(uuid: String): Option[RegistrationToken]
  def deleteToken(uuid: String): Boolean
  def deleteExpiredTokens(): Int
}

trait RegistrationService {

  /**
   * Register a new org and create a new user
   * @param orgName
   * @param username
   * @param email
   * @return
   */
  def register(orgName: String, username: String, email: String): Either[PlatformServiceError, RegistrationInfo]

  def deRegister(orgName: String): Either[PlatformServiceError, String]
}
