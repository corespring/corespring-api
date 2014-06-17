package org.corespring.v2player.integration.actionBuilders

import org.bson.types.ObjectId
import org.corespring.mongo.json.services.MongoService
import org.corespring.v2player.integration.actionBuilders.access.{ Mode, PlayerOptions }
import org.corespring.v2player.integration.actionBuilders.access.Mode.Mode
import org.corespring.v2player.integration.errors.V2Error
import org.corespring.v2player.integration.errors.Errors.{ cantLoadSession, cantParseItemId }
import org.slf4j.LoggerFactory
import play.api.mvc._

/*abstract class AuthItemCheckPermissions(
  sessionService: MongoService, auth: AuthCheck)
  extends AuthenticatedItem {
  /**
   * get an auth failure result - if no failure return None
   * @param itemId
   * @param rh
   * @return maybe an Auth Failure Result
   */
  override def authenticationFailedResult(itemId: String, rh: RequestHeader): Option[SimpleResult] = {

    val result = auth.hasAccess(
      rh,
      auth.orgCanAccessItem(itemId, _),
      auth.hasPermissions(itemId, None, Mode.gather, _))

    result match {
      case Success(true) => None
      case _ => Some(play.api.mvc.Results.BadRequest(Json.obj("error" -> "Access not granted")))
    }
  }
}*/

abstract class AuthSessionActionsCheckPermissions(sessionService: MongoService, auth: AuthCheck)
  extends SessionAuth {

  import scalaz.Scalaz._
  import scalaz._
  lazy val logger = LoggerFactory.getLogger("v2.actions")

  protected def hasPermissionForSession(id: String, mode: Mode, options: PlayerOptions): Validation[String, Boolean] = {
    val out: Validation[String, Boolean] = for {
      s <- sessionService.load(id).toSuccess(s"Can't load session with id: $id")
      itemId <- (s \ "itemId").asOpt[String].toSuccess(s"No item id defined for session $id")
    } yield {
      val b: Boolean = auth.hasPermissions(itemId, Some(id), mode, options) match {
        case Success(b) => true
        case Failure(msg) => {
          logger.warn(msg)
          false
        }
      }
      b
    }
    out
  }

  protected def orgCanAccessSession(sessionId: String, orgId: ObjectId): Validation[V2Error, Boolean] = for {
    session <- sessionService.load(sessionId).toSuccess(cantLoadSession(sessionId))
    itemId <- (session \ "itemId").asOpt[String].toSuccess(cantParseItemId)
    canAccess <- auth.orgCanAccessItem(itemId, orgId)
  } yield canAccess

  override def read(sessionId: String)(implicit header: RequestHeader): Validation[String, Boolean] = {
    val mode = header.getQueryString("mode").map(_.toString).map(Mode.withName).getOrElse(Mode.view)
    auth.hasAccess(header, orgCanAccessSession(sessionId, _), hasPermissionForSession(sessionId, mode, _)) match {
      case Success(true) => Success(true)
      case Success(false) => Failure("Authentication failed")
      case Failure(error) => Failure(error.message)
    }
  }

  override def createSession(itemId: String)(implicit header: RequestHeader): Validation[String, Boolean] = {
    logger.trace(s"createSessionHandleNotAuthorized: $itemId")
    auth.hasAccess(header, auth.orgCanAccessItem(itemId, _), auth.hasPermissions(itemId, None, Mode.gather, _)) match {
      case Success(true) => Success(true)
      case Success(false) => Failure("Didn't work")
      case Failure(error) => Failure(error.message)
    }
  }

  override def loadPlayerForSession(sessionId: String)(implicit header: RequestHeader): Validation[String, Boolean] = {
    val mode = header.getQueryString("mode").map(_.toString).map(Mode.withName).getOrElse(Mode.view)
    logger.debug(s"loadPlayerForSession $sessionId")
    auth.hasAccess(header, orgCanAccessSession(sessionId, _), hasPermissionForSession(sessionId, mode, _)) match {
      case Success(true) => {
        logger.trace(s"loadPlayerForSession success")
        Success(true)
      }
      case Success(false) => {
        logger.trace(s"loadPlayerForSession failure")
        Failure("Unauthorized")
      }
      case Failure(err) => {
        logger.trace(s"loadPlayerForSession failure: $err")
        Failure(err.message)
      }
    }
  }

}

