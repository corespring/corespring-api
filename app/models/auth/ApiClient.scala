package models.auth

import org.bson.types.ObjectId
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao._
import play.api.Play.current
import se.radley.plugin.salat._
import models.mongoContext._
import common.encryption.ShaHash
import play.api.libs.oauth.OAuth
import controllers.auth.OAuthConstants


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
  val dao = new SalatDAO[ApiClient, ObjectId](collection = collection) {}

  def findBySignature(grantType:String,clientId:String,clientSignature:String,algorithm:String,scope:Option[String]):Option[ApiClient] = {
    findByKey(clientId) match {
      case Some(apiClient) => {
        val message = grantType+":"+clientId+":"+algorithm+scope.map(":"+_).getOrElse("")
        val signature = ShaHash.sign(message,apiClient.clientSecret)
        if (clientSignature == signature) Some(apiClient)
        else None
      }
      case None => None
    }
  }
  /**
   * Retrieves an ApiClient by client id and secret from the services.
   * @param id - the client id
   * @param secret - the client secret
   * @return an Option[ApiClient]
   */
  def findByIdAndSecret(id: String, secret: String): Option[ApiClient] = {
//    findByKey(id) match {
//      case Some(apiClient) => {
//        val hash = ShaHash.sign(apiClient.clientId.toString,apiClient.clientSecret)
//        if (signature == hash) Some(apiClient)
//        else None
//      }
//      case None => None
//    }
    val idsObj = MongoDBObject(clientId -> new ObjectId(id), clientSecret -> secret)
    findOne(idsObj)
  }

  def findByKey(key:String) : Option[ApiClient] = {
    findOne(MongoDBObject(ApiClient.clientId -> new ObjectId(key)))
  }

  def findOneByOrgId(orgId:ObjectId):Option[ApiClient] = ApiClient.findOne(MongoDBObject(ApiClient.orgId -> orgId))
}