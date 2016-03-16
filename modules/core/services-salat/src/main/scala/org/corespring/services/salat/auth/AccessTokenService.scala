package org.corespring.services.salat.auth

import com.mongodb.casbah.WriteConcern
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.Context
import com.novus.salat.dao.{ SalatDAO, SalatInsertError, SalatRemoveError }
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.errors.{ GeneralError, PlatformServiceError }
import org.corespring.models.Organization
import org.corespring.models.appConfig.AccessTokenConfig
import org.corespring.models.auth.{ AccessToken, ApiClient }
import org.corespring.services.salat.HasDao
import org.corespring.{ services => interface }
import org.joda.time.DateTime

import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

class AccessTokenService(
  val dao: SalatDAO[AccessToken, ObjectId],
  val context: Context,
  orgService: interface.OrganizationService,
  config: AccessTokenConfig)
  extends interface.auth.AccessTokenService
  with interface.auth.UpdateAccessTokenService
  with HasDao[AccessToken, ObjectId] {

  private val logger = Logger[AccessTokenService]()

  object Keys {
    val tokenId = "tokenId"
    val organization = "organization"
    val scope = "scope"
  }

  // Not sure when to call this.
  protected def index = Seq(
    MongoDBObject("tokenId" -> 1),
    MongoDBObject("organization" -> 1, "tokenId" -> 1, "creationDate" -> 1, "expirationDate" -> 1, "neverExpire" -> 1)).foreach(dao.collection.ensureIndex(_))

  dao.collection.ensureIndex(MongoDBObject("tokenId" -> 1), MongoDBObject("unique" -> true))

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

  private def mkToken(apiClient: ApiClient) = {
    val creationTime = DateTime.now()
    AccessToken(apiClient.clientId, apiClient.orgId, None, ObjectId.get.toString, creationTime, creationTime.plusHours(config.tokenDurationInHours))
  }

  override def createToken(apiClient: ApiClient): Validation[PlatformServiceError, AccessToken] =
    for {
      token <- Success(mkToken(apiClient))
      insertedToken <- insertToken(token)
    } yield insertedToken

  private def insertToken(token: AccessToken): Validation[PlatformServiceError, AccessToken] = {
    try {
      //TODO: Is this writeConcern safe enough to remove the loading of the item?
      dao.insert(token, WriteConcern.Safe) match {
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

  private def invalidToken(t: String) = GeneralError(s"Invalid token: $t", None)
  private def expiredToken(t: AccessToken) = GeneralError(s"Expired token: ${t.expirationDate}", None)

  override def update(token: AccessToken): Validation[PlatformServiceError, AccessToken] = {
    logger.debug(s"function=update, token=$token")
    implicit val ctx = context
    val updateDbo = com.novus.salat.grater[AccessToken].asDBObject(token)
    val result = dao.update(MongoDBObject("tokenId" -> token.tokenId), updateDbo, upsert = false, multi = false)
    if (result.getLastError.ok) {
      Success(token)
    } else {
      Failure(PlatformServiceError(result.getLastError.getErrorMessage))
    }
  }

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
