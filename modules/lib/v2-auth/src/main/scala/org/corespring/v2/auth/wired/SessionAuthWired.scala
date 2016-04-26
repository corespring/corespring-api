package org.corespring.v2.auth.wired

import org.bson.types.ObjectId
import org.corespring.models.item.PlayerDefinition
import org.corespring.v2.auth.SessionAuth.Session
import org.corespring.v2.auth.models.{AuthMode, IdentityJson, OrgAndOpts, PlayerAccessSettings}
import org.corespring.v2.auth.{ItemAuth, PlayerDefinitionLoader,  SessionAuth}
import org.corespring.v2.errors.Errors.{cannotLoadSessionCount, cantLoadSession, errorSaving}
import org.corespring.v2.errors.V2Error
import org.corespring.v2.sessiondb.{SessionService, SessionServices}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}

import scalaz.Scalaz._
import scalaz.{Failure, Success, Validation}

class SessionAuthWired(
  itemAuth: ItemAuth[OrgAndOpts],
  pdLoader: PlayerDefinitionLoader,
  sessionServices: SessionServices) extends SessionAuth[OrgAndOpts, PlayerDefinition] {

  lazy val logger = Logger(classOf[SessionAuthWired])

  private def sessionService(implicit identity: OrgAndOpts): SessionService = if (identity.authMode == AuthMode.UserSession) {
    logger.debug("Using preview session service")
    sessionServices.preview
  } else {
    logger.debug("Using main session service")
    sessionServices.main
  }

  override def loadForRead(sessionId: String)(implicit identity: OrgAndOpts): Validation[V2Error, (JsValue, PlayerDefinition)] =
    for {
      json <- loadSessionJson(sessionId)
      playerDef <- loadPlayerDefinition(sessionId, json)
    } yield (cleanSession(json), playerDef)

  override def loadForSave(sessionId: String)(implicit identity: OrgAndOpts): Validation[V2Error, JsValue] =
    for {
      json <- loadSessionJson(sessionId)
    } yield cleanSession(json)

  override def loadForWrite(sessionId: String)(implicit identity: OrgAndOpts): Validation[V2Error, (JsValue, PlayerDefinition)] =
    for {
      json <- loadSessionJson(sessionId)
      playerDef <- loadPlayerDefinition(sessionId, json)
    } yield (cleanSession(json), playerDef)

  override def loadWithIdentity(sessionId: String)(implicit identity: OrgAndOpts): Validation[V2Error, (JsValue, PlayerDefinition)] =
    for {
      json <- loadSessionJson(sessionId)
      playerDef <- loadPlayerDefinition(sessionId, json)
    } yield (cleanSession(json, withIdentity = true), playerDef)

  private def loadSessionJson(sessionId: String)(implicit identity: OrgAndOpts): Validation[V2Error, JsValue] = {
    logger.debug(s"[loadSessionJson] $sessionId")
    (sessionService.load(sessionId) match {
      case Some(session) => Some(session)
      case _ => sessionServices.preview.load(sessionId)
    }).toSuccess(cantLoadSession(sessionId))
  }

  private def cleanSession(json: JsValue, withIdentity: Boolean = false) = {
    /** if the session contains the data - we need to trim it so it doesn't reach the client */
    withIdentity match {
      case true => json.as[JsObject] - "item"
      case _ => json.as[JsObject] - "item" - "identity"
    }
  }

  private def loadPlayerDefinition(sessionId: String, session: JsValue)(implicit identity: OrgAndOpts): Validation[V2Error, PlayerDefinition] = {
    pdLoader.loadPlayerDefinition(sessionId, session)
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
      original <- sessionServices.main.load(sessionId).toSuccess(cantLoadSession(sessionId))
      copy <- sessionServices.preview.create(original).toSuccess(cantLoadSession(sessionId))
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

  override def orgCount(orgId: ObjectId, month: DateTime)(implicit identity: OrgAndOpts): Validation[V2Error, Map[DateTime, Long]] = {
    sessionServices.main.orgCount(orgId, month) match {
      case Some(result) => Success(result)
      case _ => Failure(cannotLoadSessionCount(orgId, month))
    }
  }
}
