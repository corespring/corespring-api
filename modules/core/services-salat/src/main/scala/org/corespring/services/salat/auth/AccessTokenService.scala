package org.corespring.services.salat.auth

import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao.{ SalatInsertError, SalatRemoveError }
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.models.auth.AccessToken
import org.corespring.models.{ Organization }
import org.corespring.services.errors.PlatformServiceError
import org.corespring.services.salat.HasDao
import org.corespring.{ services => interface }
import org.joda.time.DateTime

trait AccessTokenService extends interface.auth.AccessTokenService with HasDao[AccessToken, ObjectId] {

  private val logger = Logger[AccessTokenService]()

  object Keys {
    val tokenId = "tokenId"
    val organization = "organization"
    val scope = "scope"
  }

  def tokenDurationInHours: Int

  // Not sure when to call this.
  def index = Seq(
    MongoDBObject("tokenId" -> 1),
    MongoDBObject("organization" -> 1, "tokenId" -> 1, "creationDate" -> 1, "expirationDate" -> 1, "neverExpire" -> 1)
  ).foreach(dao.collection.ensureIndex(_))

  override def removeToken(tokenId: String): Either[PlatformServiceError, Unit] = {
    logger.info(s"function=removeToken tokenId=$tokenId")

    try {
      dao.remove(MongoDBObject(Keys.tokenId -> tokenId))
      Right(())
    } catch {
      case e: SalatRemoveError => Left(PlatformServiceError("error removing token with id " + tokenId, e))
    }
  }

  /**
   * Finds an access token by id
   *
   * @param tokenId - the access token id
   * @return returns an Option[AccessToken]
   */
  override def findByTokenId(tokenId: String): Option[AccessToken] = dao.findOne(MongoDBObject(Keys.tokenId -> tokenId))

  override def getTokenForOrgById(orgId: ObjectId): Option[AccessToken] = findByOrgId(orgId)

  lazy val tokenDuration = ConfigFactory.load().getString("TOKEN_DURATION").toInt

  override def getOrCreateToken(org: Organization): AccessToken = {

    dao.findOne(MongoDBObject("orgId" -> org.id)) match {
      case Some(t) if (!t.isExpired) => t
      case _ => {
        val now = DateTime.now()
        val token: AccessToken = new AccessToken(
          organization = org.id,
          scope = None,
          tokenId = new ObjectId().toString,
          creationDate = now,
          expirationDate = now.plusHours(tokenDuration))
        dao.insert(token)
        token
      }
    }

  }

  override def findByOrgId(orgId: ObjectId): Option[AccessToken] = {
    find(orgId, None) match {
      case Some(t) if (!t.isExpired) => Some(t)
      case _ => {
        val now = DateTime.now()
        val token: AccessToken = new AccessToken(
          organization = orgId,
          scope = None,
          tokenId = new ObjectId().toString,
          creationDate = now,
          expirationDate = now.plusHours(tokenDurationInHours))
        dao.insert(token)
        Some(token)
      }
    }

  }

  override def insertToken(token: AccessToken): Either[PlatformServiceError, AccessToken] = {
    try {
      //TODO: Just do a SAFE WriteConcern instead
      dao.insert(token) match {
        case Some(id) => dao.findOneById(id) match {
          case Some(dbtoken) => Right(dbtoken)
          case None => Left(PlatformServiceError("could not retrieve token that was just inserted"))
        }
        case None => Left(PlatformServiceError("error occurred during insert"))
      }
    } catch {
      case e: SalatInsertError => Left(PlatformServiceError("error occurred during insert", e))
    }
  }

  /**
   * Finds an access token by organization and scope
   *
   * @param orgId - the organization that the token was created for
   * @param scope - the scope requested when the access token was created
   * @return returns an Option[AccessToken]
   */
  override def find(orgId: ObjectId, scope: Option[String]): Option[AccessToken] = {
    var query = MongoDBObject.newBuilder
    query += (Keys.organization -> orgId)
    if (scope.isDefined) query += (Keys.scope -> scope.get)
    dao.findOne(query.result())
  }
}
