package org.corespring.services.auth

import org.bson.types.ObjectId
import org.corespring.models.auth.ApiClient

import scalaz.Validation

trait ApiClientService {

  def getOrCreateForOrg(orgId: ObjectId): Validation[String, ApiClient]

  def findByClientId(id: String): Option[ApiClient]

  /**
   * Retrieves an ApiClient by client id and secret from the services.
   * @param id - the client id
   * @param secret - the client secret
   * @return an Option[ApiClient]
   */
  def findByClientIdAndSecret(id: String, secret: String): Option[ApiClient]

  def findByOrgId(orgId: ObjectId): Stream[ApiClient]
}
