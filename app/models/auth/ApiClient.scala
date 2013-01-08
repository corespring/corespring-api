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
case class ApiClient(orgId: ObjectId, clientId: ObjectId, clientSecret: String, username:Option[String] = None)

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
  def findOne(orgId:ObjectId, optusername:Option[String]):Option[ApiClient] = {
    optusername match {
      case Some(username) => ApiClient.findOne(MongoDBObject(ApiClient.orgId -> orgId, ApiClient.username -> username))
      case None => ApiClient.findOne(MongoDBObject(ApiClient.orgId -> orgId))
    }
  }
}