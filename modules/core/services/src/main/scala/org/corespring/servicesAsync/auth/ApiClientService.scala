package org.corespring.servicesAsync.auth

import org.bson.types.ObjectId
import org.corespring.models.auth.ApiClient

import scalaz.Validation
import scala.concurrent.Future

trait ApiClientService {

  protected val KEY_LENGTH = 16
  protected val KEY_RADIX = 36

  def generateTokenId(keyLength: Int = KEY_LENGTH): Future[String]
  def getOrCreateForOrg(orgId: ObjectId): Future[Validation[String, ApiClient]]

  def findByKey(key: String): Future[Option[ApiClient]]

  /**
   * Retrieves an ApiClient by client id and secret from the services.
   * @param id - the client id
   * @param secret - the client secret
   * @return an Option[ApiClient]
   */
  def findByIdAndSecret(id: String, secret: String): Future[Option[ApiClient]]

  def findOneByOrgId(orgId: ObjectId): Future[Option[ApiClient]]
}
