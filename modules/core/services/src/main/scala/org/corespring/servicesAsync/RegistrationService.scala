package org.corespring.servicesAsync

import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.models.Organization
import org.corespring.models.auth.ApiClient
import org.corespring.models.registration.RegistrationToken
import scala.concurrent.Future

case class RegistrationInfo(org: Organization, defaultCollection: ObjectId, apiClient: ApiClient)

trait RegistrationTokenService {

  def createToken(token: RegistrationToken): Future[Boolean]
  def findTokenByUuid(uuid: String): Future[Option[RegistrationToken]]
  def deleteTokenUuid(uuid: String): Future[Boolean]
  def deleteExpiredTokens(): Future[Int]
}

trait RegistrationService {

  /**
   * Register a new org and create a new user
   * @param orgName
   * @param username
   * @param email
   * @return
   */
  def register(orgName: String, username: String, email: String): Future[Either[PlatformServiceError, RegistrationInfo]]

  def deRegister(orgName: String): Future[Either[PlatformServiceError, String]]
}
