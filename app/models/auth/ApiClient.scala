package models.auth

import org.bson.types.ObjectId
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao._
import play.api.Play.current
import se.radley.plugin.salat._
import models.mongoContext._
import controllers.auth.Permission


/**
 * An API client.  This gets created for each organization that is allowed API access
 */
case class ApiClient(orgId: ObjectId, clientId: ObjectId, clientSecret: String)

object ApiClient extends ModelCompanion[ApiClient, ObjectId] {
  val orgId = "orgId"
  val clientId = "clientId"
  val clientSecret = "clientSecret"
  val username = "username"

  val collection = mongoCollection("apiClients")
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