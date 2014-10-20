package org.corespring.v2.auth.wired

import org.bson.types.ObjectId
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.{ PlayerDefinition, Item }
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.auth.SessionAuth.Session
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.auth.{ ItemAuth, SessionAuth }
import org.corespring.v2.errors.Errors.{ cantLoadSession, errorSaving, noItemIdInSession }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.libs.json.{ JsObject, JsValue }

import scalaz.Scalaz._
import scalaz.{ Success, Validation }

trait SessionAuthWired extends SessionAuth[OrgAndOpts, PlayerDefinition] {

  lazy val logger = V2LoggerFactory.getLogger("auth.SessionAuth")

  def itemTransformer: ItemTransformer

  def itemAuth: ItemAuth[OrgAndOpts]

  /**
   * The main session service holds 'real' item sessions
   * @return
   */
  def mainSessionService: MongoService

  /**
   * The preview session service holds 'preview' sessions -
   * This service is used when the identity -> AuthMode == UserSession
   * @return
   */
  def previewSessionService: MongoService

  private def sessionService(implicit identity: OrgAndOpts): MongoService = if (identity.authMode == AuthMode.UserSession) {
    logger.debug("Using preview session service")
    previewSessionService
  } else {
    logger.debug("Using main session service")
    mainSessionService
  }

  def hasPermissions(itemId: String, sessionId: String, settings: PlayerAccessSettings): Validation[V2Error, Boolean]

  override def loadForRead(sessionId: String)(implicit identity: OrgAndOpts): Validation[V2Error, (JsValue, PlayerDefinition)] = load(sessionId)

  override def loadForWrite(sessionId: String)(implicit identity: OrgAndOpts): Validation[V2Error, (JsValue, PlayerDefinition)] = load(sessionId)

  private def load(sessionId: String)(implicit identity: OrgAndOpts): Validation[V2Error, (JsValue, PlayerDefinition)] = {

    logger.debug(s"[load] $sessionId")

    val out = for {
      json <- sessionService.load(sessionId).toSuccess(cantLoadSession(sessionId))
      playerDef <- loadPlayerDefinition(sessionId, json)
    } yield (json, playerDef)
    logger.trace(s"loadFor sessionId: $sessionId - result successful")
    out
  }

  private def loadPlayerDefinition(sessionId: String, session: JsValue)(implicit identity: OrgAndOpts): Validation[V2Error, PlayerDefinition] = {

    def loadContentItem: Validation[V2Error, Item] = {
      for {
        itemId <- (session \ "itemId").asOpt[String].toSuccess(noItemIdInSession(sessionId))
        item <- itemAuth.loadForRead(itemId)
        hasPerms <- hasPermissions(item.id.toString, sessionId, identity.opts)
      } yield item
    }

    val sessionPlayerDef: Option[PlayerDefinition] = (session \ "item").asOpt[JsObject].map {
      internalModel =>
        internalModel.asOpt[PlayerDefinition]
    }.flatten

    sessionPlayerDef
      .map { d => Success(d) }
      .getOrElse {
        loadContentItem.map { i =>
          itemTransformer.createPlayerDefinition(i)
        }
      }
  }
  override def canCreate(itemId: String)(implicit identity: OrgAndOpts): Validation[V2Error, Boolean] = {
    itemAuth.loadForRead(itemId).map { i => true }
  }

  override def saveSession(implicit identity: OrgAndOpts): Validation[V2Error, (String, Session) => Option[Session]] = Success(sessionService.save)

  override def create(session: Session)(implicit identity: OrgAndOpts): Validation[V2Error, ObjectId] = sessionService.create(session).toSuccess(errorSaving)

}
