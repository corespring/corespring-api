package org.corespring.v2.auth.wired

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.{ PlayerDefinition, Item }
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.auth.SessionAuth.Session
import org.corespring.v2.auth.models.{ IdentityJson, AuthMode, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.auth.{ ItemAuth, SessionAuth }
import org.corespring.v2.errors.Errors.{ cantLoadSession, errorSaving, noItemIdInSession }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import org.corespring.v2.sessiondb.SessionService
import org.joda.time.{ DateTime, DateTimeZone }
import play.api.libs.json.{ Json, JsObject, JsValue }

import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

trait SessionAuthWired extends SessionAuth[OrgAndOpts, PlayerDefinition] {

  lazy val logger = V2LoggerFactory.getLogger("auth.SessionAuth")

  def itemTransformer: ItemTransformer

  def itemAuth: ItemAuth[OrgAndOpts]

  /**
   * The main session service holds 'real' item sessions
   * @return
   */
  def mainSessionService: SessionService

  /**
   * The preview session service holds 'preview' sessions -
   * This service is used when the identity -> AuthMode == UserSession
   * @return
   */
  def previewSessionService: SessionService

  private def sessionService(implicit identity: OrgAndOpts): SessionService = if (identity.authMode == AuthMode.UserSession) {
    logger.debug("Using preview session service")
    previewSessionService
  } else {
    logger.debug("Using main session service")
    mainSessionService
  }

  def hasPermissions(itemId: String, sessionId: Option[String], settings: PlayerAccessSettings): Validation[V2Error, Boolean]

  override def loadForRead(sessionId: String)(implicit identity: OrgAndOpts): Validation[V2Error, (JsValue, PlayerDefinition)] = load(sessionId)

  override def loadForWrite(sessionId: String)(implicit identity: OrgAndOpts): Validation[V2Error, (JsValue, PlayerDefinition)] = load(sessionId)

  override def loadWithIdentity(sessionId: String)(implicit identity: OrgAndOpts): Validation[V2Error, (JsValue, PlayerDefinition)] = load(sessionId, withIdentity = true)

  private def load(sessionId: String, withIdentity: Boolean = false)(implicit identity: OrgAndOpts): Validation[V2Error, (JsValue, PlayerDefinition)] = {

    logger.debug(s"[load] $sessionId")

    val out = for {
      json <- (sessionService.load(sessionId) match {
        case Some(session) => Some(session)
        case _ => previewSessionService.load(sessionId)
      }).toSuccess(cantLoadSession(sessionId))
      playerDef <- loadPlayerDefinition(sessionId, json)
    } yield {
      /** if the session contains the data - we need to trim it so it doesn't reach the client */
      val cleanedSession = withIdentity match {
        case true => json.as[JsObject] - "item"
        case _ => json.as[JsObject] - "item" - "identity"
      }
      (cleanedSession, playerDef)
    }

    logger.trace(s"loadFor sessionId: $sessionId - result successful")
    out
  }

  override def reopen(sessionId: String)(implicit identity: OrgAndOpts): Validation[V2Error, Session] = for {
    reopenedSession <- sessionService.load(sessionId)
      .map(_.as[JsObject] ++ Json.obj("isComplete" -> false, "attempts" -> 0)).toSuccess(cantLoadSession(sessionId))
    savedReopened <- sessionService.save(sessionId, reopenedSession).toSuccess(errorSaving)
  } yield {
    savedReopened
  }

  override def complete(sessionId: String)(implicit identity: OrgAndOpts): Validation[V2Error, Session] = for {
    completedSession <- sessionService.load(sessionId)
      .map(_.as[JsObject] ++ Json.obj("isComplete" -> true)).toSuccess(cantLoadSession(sessionId))
    savedCompleted <- sessionService.save(sessionId, completedSession).toSuccess(errorSaving)
  } yield {
    savedCompleted
  }

  private def loadPlayerDefinition(sessionId: String, session: JsValue)(implicit identity: OrgAndOpts): Validation[V2Error, PlayerDefinition] = {

    def loadContentItem: Validation[V2Error, Item] = {
      for {
        itemId <- (session \ "itemId").asOpt[String].toSuccess(noItemIdInSession(sessionId))
        item <- itemAuth.loadForRead(itemId)
        hasPerms <- hasPermissions(item.id.toString, Some(sessionId), identity.opts)
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

  override def saveSessionFunction(implicit identity: OrgAndOpts): Validation[V2Error, (String, Session) => Option[Session]] = Success((id, session) => {
    val withIdentityData = addIdentityToSession(session, identity)
    sessionService.save(id, withIdentityData).map(result => rmIdentityFromSession(result))
  })

  override def create(session: Session)(implicit identity: OrgAndOpts): Validation[V2Error, ObjectId] = {
    val withIdentityData = dateCreated ++ addIdentityToSession(session, identity)
    sessionService.create(withIdentityData).toSuccess(errorSaving)
  }

  override def cloneIntoPreview(sessionId: String)(implicit identity: OrgAndOpts): Validation[V2Error, ObjectId] = {
    for {
      original <- mainSessionService.load(sessionId).toSuccess(cantLoadSession(sessionId))
      copy <- previewSessionService.create(original).toSuccess(cantLoadSession(sessionId))
    } yield {
      copy
    }
  }

  private def dateCreated: JsObject = Json.obj(
    "dateCreated" -> Json.obj(
      "$date" -> DateTime.now(DateTimeZone.UTC)))

  private def addIdentityToSession(session: Session, identity: OrgAndOpts): JsObject =
    session.as[JsObject] ++ Json.obj("identity" -> IdentityJson(identity))

  private def rmIdentityFromSession(s: Session) = s.asInstanceOf[JsObject] - "identity"
}
