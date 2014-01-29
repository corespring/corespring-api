package org.corespring.v2player.integration.actionBuilders

import play.api.mvc._
import scala.concurrent.{ Await, Future }
import java.util.concurrent.TimeUnit

/**
 * Wrap AuthenticatedSessionActions to check for DEV_TOOLS_ENABLED if not present call the underlying actions
 * @param underlying
 */
class DevToolsSessionActions(underlying: AuthenticatedSessionActions) extends AuthenticatedSessionActions {

  private def run(block: Request[AnyContent] => Result)(fn: Request[AnyContent] => Future[SimpleResult]): Action[AnyContent] = Action {
    request =>
      request.session.get("DEV_TOOLS_ENABLED") match {
        case Some("true") => block(request)
        case _ => {
          import scala.concurrent.duration._
          Await.result(fn(request), FiniteDuration(3, TimeUnit.SECONDS))
        }
      }
  }

  override def read(sessionId: String)(block: (Request[AnyContent]) => Result): Action[AnyContent] = run(block) {
    (r: Request[AnyContent]) => underlying.read(sessionId)(block)(r)
  }

  override def loadPlayerForSession(sessionId: String)(error: (Int, String) => Result)(block: (Request[AnyContent]) => Result): Action[AnyContent] = run(block) {
    r =>
      underlying.loadPlayerForSession(sessionId)(error)(block)(r)
  }

  /**
   * The client wants to create a session for the given item id.
   * call authorized if successful, failed if not
   */
  override def createSessionHandleNotAuthorized(itemId: String)(authorized: (Request[AnyContent]) => Result)(failed: (Request[AnyContent], Int, String) => Result): Action[AnyContent] =
    run(authorized) {
      r =>
        underlying.createSessionHandleNotAuthorized(itemId)(authorized)(failed)(r)
    }

}
