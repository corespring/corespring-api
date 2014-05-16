package org.corespring.v2player.integration.actionBuilders

import org.bson.types.ObjectId
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.services.UserService
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.v2player.integration.actionBuilders.access.Mode.Mode
import org.corespring.v2player.integration.actionBuilders.access.{ Mode, PlayerOptions }
import play.api.mvc._
import scala.Some
import org.corespring.v2player.integration.errors.Errors.{ cantLoadSession, cantParseItemId }
import org.corespring.v2player.integration.errors.V2Error
import scalaz.Success
import play.api.libs.json.Json
import org.corespring.platform.core.controllers.auth.SecureSocialService

abstract class AuthItemCheckPermissions(
  secureSocialService: SecureSocialService,
  userService: UserService,
  sessionService: MongoService,
  itemService: ItemService,
  orgService: OrganizationService)
  extends BaseAuth(secureSocialService, userService, sessionService, itemService, orgService)
  with AuthenticatedItem {
  /**
   * get an auth failure result - if no failure return None
   * @param itemId
   * @param rh
   * @return maybe an Auth Failure Result
   */
  override def authenticationFailedResult(itemId: String, rh: RequestHeader): Option[SimpleResult] = {

    val result = checkAccess(
      rh,
      orgCanAccessItem(itemId, _),
      hasPermissions(itemId, None, Mode.gather, _))

    result match {
      case Success(true) => None
      case _ => Some(play.api.mvc.Results.BadRequest(Json.obj("error" -> "Access not granted")))
    }
  }
}

abstract class AuthSessionActionsCheckPermissions(
  secureSocialService: SecureSocialService,
  userService: UserService,
  sessionService: MongoService,
  itemService: ItemService,
  orgService: OrganizationService)
  extends BaseAuth(secureSocialService, userService, sessionService, itemService, orgService)
  with AuthenticatedSessionActions {

  import play.api.http.Status._
  import play.api.mvc.Results._
  import scalaz.Scalaz._
  import scalaz._

  override def loggerName = "org.corespring.v2player.integration.actionBuilders.AuthenticatedSessionActionsCheckUserAndPermissions"

  protected def hasPermissionForSession(id: String, mode: Mode, options: PlayerOptions): Validation[String, Boolean] = {
    val out: Validation[String, Boolean] = for {
      s <- sessionService.load(id).toSuccess(s"Can't load session with id: $id")
      itemId <- (s \ "itemId").asOpt[String].toSuccess(s"No item id defined for session $id")
    } yield {
      val b: Boolean = hasPermissions(itemId, Some(id), mode, options) match {
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
    canAccess <- orgCanAccessItem(itemId, orgId)
  } yield canAccess

  override def read(sessionId: String)(block: (Request[AnyContent]) => Result): Action[AnyContent] = Action {
    request =>
      val mode = request.getQueryString("mode").map(_.toString).map(Mode.withName).getOrElse(Mode.view)
      checkAccess(request, orgCanAccessSession(sessionId, _), hasPermissionForSession(sessionId, mode, _)) match {
        case Success(true) => block(request)
        case Success(false) => Unauthorized("Authentication failed")
        case Failure(error) => Status(error.code)(error.message)
      }
  }

  def createSessionHandleNotAuthorized(itemId: String)(authorized: (Request[AnyContent]) => Result)(failed: (Request[AnyContent], Int, String) => Result): Action[AnyContent] = Action {
    request =>
      logger.trace(s"createSessionHandleNotAuthorized: $itemId")
      checkAccess(request, orgCanAccessItem(itemId, _), hasPermissions(itemId, None, Mode.gather, _)) match {
        case Success(true) => authorized(request)
        case Success(false) => failed(request, BAD_REQUEST, "Didn't work")
        case Failure(error) => failed(request, error.code, error.message)
      }
  }

  override def loadPlayerForSession(sessionId: String)(error: (Int, String) => Result)(block: (Request[AnyContent]) => Result): Action[AnyContent] = Action {
    request =>
      val mode = request.getQueryString("mode").map(_.toString).map(Mode.withName).getOrElse(Mode.view)
      logger.debug(s"loadPlayerForSession $sessionId")
      checkAccess(request, orgCanAccessSession(sessionId, _), hasPermissionForSession(sessionId, mode, _)) match {
        case Success(true) => {
          logger.trace(s"loadPlayerForSession success")
          block(request)
        }
        case Success(false) => {
          logger.trace(s"loadPlayerForSession failure")
          error(UNAUTHORIZED, "Message")
        }
        case Failure(err) => {
          logger.trace(s"loadPlayerForSession failure: $error")
          error(err.code, err.message)
        }
      }
  }
}

