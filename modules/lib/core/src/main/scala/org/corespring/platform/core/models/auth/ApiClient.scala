package org.corespring.platform.core.models.auth

import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao._
import org.bson.types.ObjectId
import org.corespring.common.encryption.ShaHash
import org.slf4j.LoggerFactory
import play.api.Play.current
import se.radley.plugin.salat._

object OAuthConstants {
  val GrantType = "grant_type"
  val ClientCredentials = "client_credentials"
  val ClientId = "client_id"
  val ClientSecret = "client_secret"
  val ClientSignature = "client_signature"
  val HashAlgorithm = "algorithm"
  val Sha1Hash = "HmacSHA1"
  val Scope = "scope"
  val Organization = "organization"
  val AccessToken = "access_token"
  val Error = "error"
  val Code = "code"
  val Message = "messsage"
}

/**
 * An API client.  This gets created for each organization that is allowed API access
 */
case class ApiClient(orgId: ObjectId, clientId: ObjectId, clientSecret: String) {
  def defaultClientSignature: String = ShaHash.sign(
    OAuthConstants.ClientCredentials + ":" + clientId + ":" + OAuthConstants.Sha1Hash,
    clientSecret)
}

trait ApiClientService {

  def findByKey(key: String): Option[ApiClient]
}

object ApiClient extends ApiClientService with ModelCompanion[ApiClient, ObjectId] {

  lazy val logger = LoggerFactory.getLogger("org.corespring.core.ApiClient")
  val orgId = "orgId"
  val clientId = "clientId"
  val clientSecret = "clientSecret"

  val collection = mongoCollection("apiClients")

  import org.corespring.platform.core.models.mongoContext.context

  //collection.ensureIndex( keys = MongoDBObject("clientId" -> 1), "apiClientIndex", unique = true)

  val dao = new SalatDAO[ApiClient, ObjectId](collection = collection) {}

  /**
   * Retrieves an ApiClient by client id and secret from the services.
   * @param id - the client id
   * @param secret - the client secret
   * @return an Option[ApiClient]
   */
  def findByIdAndSecret(id: String, secret: String): Option[ApiClient] = {
    val idsObj = MongoDBObject(clientId -> new ObjectId(id), clientSecret -> secret)
    findOne(idsObj)
  }

  def findByKey(key: String): Option[ApiClient] = {
    logger.trace(s"api client count:  ${ApiClient.count()}")
    findOne(MongoDBObject(ApiClient.clientId -> new ObjectId(key)))
  }

  def findOneByOrgId(orgId: ObjectId): Option[ApiClient] = ApiClient.findOne(MongoDBObject(ApiClient.orgId -> orgId))
}