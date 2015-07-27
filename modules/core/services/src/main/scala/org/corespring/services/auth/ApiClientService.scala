package org.corespring.services.auth

import org.bson.types.ObjectId
import org.corespring.models.auth.ApiClient

trait ApiClientService {

  def createForOrg(orgId:ObjectId) : Either[String,ApiClient]

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
