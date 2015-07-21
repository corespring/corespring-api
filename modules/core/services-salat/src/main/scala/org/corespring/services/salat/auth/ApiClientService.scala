package org.corespring.services.salat.auth

import com.mongodb.casbah.commons.MongoDBObject
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.models.auth.ApiClient
import org.corespring.services.salat.HasDao
import org.corespring.{ services => interface }
/**
 * Salat implementation
 */
trait ApiClientService extends interface.auth.ApiClientService with HasDao[ApiClient, ObjectId] {

  private val logger = Logger[ApiClientService]()

  object Keys {
    val clientId = "clientId"
    val clientSecret = "clientSecret"
    val orgId = "orgId"
  }

  /**
   * Retrieves an ApiClient by client id and secret from the services.
   * @param id - the client id
   * @param secret - the client secret
   * @return an Option[ApiClient]
   */
  def findByIdAndSecret(id: String, secret: String): Option[ApiClient] = {
    val idsObj = MongoDBObject(Keys.clientId -> new ObjectId(id), Keys.clientSecret -> secret)
    dao.findOne(idsObj)
  }

  def findByKey(key: String): Option[ApiClient] = {
    logger.trace(s"api client count:  ${dao.count()}")
    dao.findOne(MongoDBObject(Keys.clientId -> new ObjectId(key)))
  }

  def findOneByOrgId(orgId: ObjectId): Option[ApiClient] = dao.findOne(MongoDBObject(Keys.orgId -> orgId))
}
