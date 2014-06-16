package org.corespring.v2player.integration.actionBuilders

import org.bson.types.ObjectId
import org.corespring.container.client.actions.{ PlayerJsRequest, PlayerLauncherActions => LaunchActions }
import org.corespring.platform.core.controllers.auth.{ SecureSocialService, UserSession }
import org.corespring.platform.core.services.UserService
import org.corespring.v2player.integration.actionBuilders.access.{ V2PlayerCookieKeys, V2PlayerCookieWriter }
import play.api.mvc._

import scalaz._

object PlayerLauncherActions {

  import play.api.http.Status._

  sealed abstract class LaunchError(val code: Int, val message: String)

  case object noClientId extends LaunchError(BAD_REQUEST, "You must specify 'apiClient'")

  case object noOptions extends LaunchError(BAD_REQUEST, "You must specify 'options'")

  case object noOrgId extends LaunchError(BAD_REQUEST, "Error getting orgId")

  case object cantDecrypt extends LaunchError(BAD_REQUEST, "Error decrypting")

  case object badJson extends LaunchError(BAD_REQUEST, "Error reading json")
}

abstract class PlayerLauncherActions(
  val secureSocialService: SecureSocialService,
  val userService: UserService)
  extends LaunchActions[AnyContent]
  with UserSession
  with LoadOrgAndOptions
  with V2PlayerCookieWriter {

  /** A helper method to allow you to create a new session out of the existing and a variable number of Key values pairs */
  override def sumSession(s: Session, keyValues: (String, String)*): Session = {
    keyValues.foldRight(s)((kv: (String, String), acc: Session) => acc + (kv._1, kv._2))
  }

  override def editorJs(block: (PlayerJsRequest[AnyContent]) => Result): Action[AnyContent] = loadJs(block)

  /**
   * Handle the request for the player js.
   * Get the orgId and player options and add them to the player session if found.
   */
  override def playerJs(block: (PlayerJsRequest[AnyContent]) => Result): Action[AnyContent] = loadJs(block)

  private def loadJs(block: PlayerJsRequest[AnyContent] => Result): Action[AnyContent] = Action {
    request =>
      getOrgIdAndOptions(request) match {
        case Success((orgId, opts)) => {
          val newSession = sumSession(request.session, playerCookies(orgId, Some(opts)): _*)
          block(new PlayerJsRequest(opts.secure, request)).withSession(newSession)
        }
        case Failure(error) => block(new PlayerJsRequest(false, request, Seq(error)))
      }

  }

  override def keys: V2PlayerCookieKeys.type = V2PlayerCookieKeys
}
