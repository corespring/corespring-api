package models.auth

import org.bson.types.ObjectId
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import com.mongodb.casbah.commons.MongoDBObject
import se.radley.plugin.salat._
import play.api.Play.current
import models.mongoContext._
import org.joda.time.DateTime
import play.api.libs.json.{JsString, JsValue, JsObject, Writes}


/**
 * An access token
 *
 * @see ApiClient
 */

case class AccessToken(organization: ObjectId, scope: Option[String], var tokenId: String, creationDate: DateTime, expirationDate: DateTime) {
  def isExpired:Boolean = {
    DateTime.now().isAfter(expirationDate)
  }
}

object AccessToken extends ModelCompanion[AccessToken, ObjectId] {
  val organization = "organization"
  val scope = "scope"
  val tokenId = "tokenId"

  val collection = mongoCollection("accessTokens")
  val dao = new SalatDAO[AccessToken, ObjectId](collection = collection) {}

  /**
   * Finds an access token by id
   *
   * @param tokenId - the access token id
   * @return returns an Option[AccessToken]
   */
  def findById(tokenId: String) = findOne(MongoDBObject(this.tokenId -> tokenId))

  /**
   * Finds an access token by organization and scope
   *
   * @param orgId - the organization that the token was created for
   * @param scope - the scope requested when the access token was created
   * @return returns an Option[AccessToken]
   */
  def find(orgId: ObjectId, scope: Option[String]) = {
    var query = MongoDBObject.newBuilder
    query += (organization -> orgId)
    if (scope.isDefined) query += (this.scope -> scope.get)
    findOne(query.result())
  }
}
