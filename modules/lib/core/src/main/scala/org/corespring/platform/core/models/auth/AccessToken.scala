package org.corespring.platform.core.models.auth

import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao.{ SalatRemoveError, SalatInsertError, SalatDAO, ModelCompanion }
import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.joda.time.DateTime
import play.api.Play.current
import se.radley.plugin.salat._
import org.corespring.platform.core.models.error.InternalError
import com.typesafe.config.ConfigFactory

/**
 * An access token
 *
 * @see ApiClient
 */

case class AccessToken(organization: ObjectId,
  scope: Option[String],
  var tokenId: String,
  creationDate: DateTime = DateTime.now(),
  expirationDate: DateTime = DateTime.now().plusHours(AccessToken.tokenDuration),
  neverExpire: Boolean = false) {
  def isExpired: Boolean = {
    !neverExpire && DateTime.now().isAfter(expirationDate)
  }
}
object AccessToken extends ModelCompanion[AccessToken, ObjectId] {
  val organization = "organization"
  val scope = "scope"
  val tokenId = "tokenId"
  lazy val tokenDuration = ConfigFactory.load().getString("TOKEN_DURATION").toInt
  val collection = mongoCollection("accessTokens")

  import org.corespring.platform.core.models.mongoContext.context

  val dao = new SalatDAO[AccessToken, ObjectId](collection = collection) {}

  def removeToken(tokenId: String): Either[InternalError, Unit] = {
    try {
      AccessToken.remove(MongoDBObject(AccessToken.tokenId -> tokenId))
      Right(())
    } catch {
      case e: SalatRemoveError => Left(InternalError("error removing token with id " + tokenId, e))
    }
  }
  def insertToken(token: AccessToken): Either[InternalError, AccessToken] = {
    try {
      AccessToken.insert(token) match {
        case Some(id) => AccessToken.findOneById(id) match {
          case Some(dbtoken) => Right(dbtoken)
          case None => Left(InternalError("could not retrieve token that was just inserted"))
        }
        case None => Left(InternalError("error occurred during insert"))
      }
    } catch {
      case e: SalatInsertError => Left(InternalError("error occurred during insert", e))
    }

  }
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

  def getTokenForOrgById(id: ObjectId): Option[AccessToken] = {
    Organization.findOneById(id).map(getTokenForOrg)
  }

  def getTokenForOrg(org: Organization): AccessToken = {

    AccessToken.find(org.id, None) match {
      case Some(t) if (!t.isExpired) => t
      case _ => {
        val now = DateTime.now()
        val token: AccessToken = new AccessToken(
          organization = org.id,
          scope = None,
          tokenId = new ObjectId().toString,
          creationDate = now,
          expirationDate = now.plusHours(tokenDuration))
        AccessToken.insert(token)
        token
      }
    }

  }
}
