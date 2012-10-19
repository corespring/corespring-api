package models.auth

import org.bson.types.ObjectId
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao._
import play.api.Play.current
import se.radley.plugin.salat._
import models.mongoContext._


/**
 * An API client.  This gets created for each organization that is allowed API access
 */
case class ApiClient(id: ObjectId, clientId: ObjectId, clientSecret: String)

object ApiClient extends ModelCompanion[ApiClient, ObjectId] {
  val clientId = "clientId"
  val clientSecret = "clientSecret"

  val collection = mongoCollection("apiClients")
  val dao = new SalatDAO[ApiClient, ObjectId](collection = collection) {}

  /**
   * Retrieves an ApiClient by client id and secret from the services.
   * @param id - the client id
   * @param secret - the client secret
   * @return an Option[ApiClient]
   */
  def findByIdAndSecret(id: String, secret: String): Option[ApiClient] = findByIdAndSecret(new ObjectId(id),secret)
  def findByIdAndSecret(id:ObjectId, secret:String):Option[ApiClient] = {
    val idsObj = MongoDBObject(clientId -> id, clientSecret -> secret)
    findOne(idsObj)
  }
}