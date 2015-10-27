package org.corespring.services.salat.auth

import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.Context
import com.novus.salat.dao.{ SalatDAO, SalatInsertError, SalatRemoveError }
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.errors.{ GeneralError, PlatformServiceError }
import org.corespring.models.Organization
import org.corespring.models.appConfig.AccessTokenConfig
import org.corespring.models.auth.AccessToken
import org.corespring.services.salat.HasDao
import org.corespring.{ services => interface }
import org.joda.time.DateTime

import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

class AccessTokenService(
  val dao: SalatDAO[AccessToken, ObjectId],
  val context: Context,
  apiClientService: interface.auth.ApiClientService,
  orgService: interface.OrganizationService,
  config: AccessTokenConfig) extends interface.auth.AccessTokenService with HasDao[AccessToken, ObjectId] {

  private val logger = Logger[AccessTokenService]()

  object Keys {
    val tokenId = "tokenId"
    val organization = "organization"
    val scope = "scope"
  }

  // Not sure when to call this.
  def index = Seq(
    MongoDBObject("tokenId" -> 1),
    MongoDBObject("organization" -> 1, "tokenId" -> 1, "creationDate" -> 1, "expirationDate" -> 1, "neverExpire" -> 1)).foreach(dao.collection.ensureIndex(_))

  override def removeToken(tokenId: String): Validation[PlatformServiceError, Unit] = {
    logger.info(s"function=removeToken tokenId=$tokenId")

    try {
      dao.remove(MongoDBObject(Keys.tokenId -> tokenId))
      Success(())
    } catch {
      case e: SalatRemoveError => Failure(PlatformServiceError("error removing token with id " + tokenId, e))
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

  private def mkToken(orgId: ObjectId) = {
    val creationTime = DateTime.now()
    AccessToken(orgId, None, apiClientService.generateTokenId(), creationTime, creationTime.plusHours(config.tokenDurationInHours))
  }

  override def createToken(clientId: String, clientSecret: String): Validation[PlatformServiceError, AccessToken] =
    for {
      apiClient <- apiClientService.findByIdAndSecret(clientId, clientSecret).toSuccess(GeneralError("No api client found", None))
      token <- Success(mkToken(apiClient.orgId))
      insertedToken <- insertToken(token)
    } yield insertedToken

  override def getOrCreateToken(orgId: ObjectId): AccessToken = {
    dao.findOne(MongoDBObject("organization" -> orgId)) match {
      case Some(t) if (!t.isExpired) => t
      case _ => {
        val token: AccessToken = mkToken(orgId)
        dao.insert(token)
        token
      }
    }
  }

  override def getOrCreateToken(org: Organization): AccessToken = getOrCreateToken(org.id)

  override def findByOrgId(orgId: ObjectId): Option[AccessToken] = {
    find(orgId, None) match {
      case Some(t) if (!t.isExpired) => Some(t)
      case _ => {
        val token: AccessToken = mkToken(orgId)
        dao.insert(token)
        Some(token)
      }
    }
  }

  override def insertToken(token: AccessToken): Validation[PlatformServiceError, AccessToken] = {
    try {
      //TODO: Just do a SAFE WriteConcern instead
      dao.insert(token, dao.collection.writeConcern) match {
        case Some(id) => dao.findOneById(id) match {
          case Some(dbtoken) => Success(dbtoken)
          case None => Failure(PlatformServiceError("could not retrieve token that was just inserted"))
        }
        case None => Failure(PlatformServiceError("error occurred during insert"))
      }
    } catch {
      case e: SalatInsertError => Failure(PlatformServiceError("error occurred during insert", e))
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

  private def invalidToken(t: String) = GeneralError(s"Invalid token: $t", None)
  private def expiredToken(t: AccessToken) = GeneralError(s"Expired token: ${t.expirationDate}", None)
  private def noOrgForToken(t: AccessToken) = GeneralError(s"Expired token: ${t.expirationDate}", None)

  override def orgForToken(token: String): Validation[PlatformServiceError, Organization] = for {
    accessToken <- findByTokenId(token).toSuccess(invalidToken(token))
    _ = logger.debug(s"function=orgForToken, tokenString=$token")
    unexpiredToken <- if (accessToken.isExpired) Failure(expiredToken(accessToken)) else Success(accessToken)
    _ = logger.trace(s"function=orgForToken, accessToken=$accessToken - is an unexpired token")
    org <- orgService.findOneById(unexpiredToken.organization).toSuccess(noOrgForToken(accessToken))
    _ = logger.trace(s"function=orgForToken, accessToken=$accessToken org=$org")
  } yield org

}
