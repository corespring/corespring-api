package org.corespring.platform.core.models.auth

import org.bson.types.ObjectId
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao._
import play.api.Play.current
import se.radley.plugin.salat._
import play.api.libs.oauth.OAuth
import org.corespring.common.encryption.ShaHash

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
case class ApiClient(orgId: ObjectId, clientId: ObjectId, clientSecret: String){
  def defaultClientSignature:String = ShaHash.sign(
      OAuthConstants.ClientCredentials+":"+clientId+":"+OAuthConstants.Sha1Hash,
      clientSecret
    )
}

object ApiClient extends ModelCompanion[ApiClient, ObjectId] {
  val orgId = "orgId"
  val clientId = "clientId"
  val clientSecret = "clientSecret"

  val collection = mongoCollection("apiClients")
  import org.corespring.platform.core.models.mongoContext.context
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

  def findByKey(key:String) : Option[ApiClient] = {
    findOne(MongoDBObject(ApiClient.clientId -> new ObjectId(key)))
  }

  def findOneByOrgId(orgId:ObjectId):Option[ApiClient] = ApiClient.findOne(MongoDBObject(ApiClient.orgId -> orgId))
}