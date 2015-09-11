package org.corespring.services.auth

import org.bson.types.ObjectId
import org.corespring.models.auth.ApiClient

import scalaz.Validation

trait ApiClientService {

  protected val KEY_LENGTH = 16
  protected val KEY_RADIX = 36

  def generateTokenId(keyLength: Int = KEY_LENGTH): String
  def getOrCreateForOrg(orgId: ObjectId): Validation[String, ApiClient]

  def findByKey(key: String): Option[ApiClient]

  /**
   * Retrieves an ApiClient by client id and secret from the services.
   * @param id - the client id
   * @param secret - the client secret
   * @return an Option[ApiClient]
   */
  def findByIdAndSecret(id: String, secret: String): Option[ApiClient]

  def findOneByOrgId(orgId: ObjectId): Option[ApiClient]
}
