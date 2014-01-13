package org.corespring.v2player.integration.actionBuilders

import play.api.mvc.{Action, Request, Result, AnyContent}

trait AuthenticatedSessionActions {
  def read(sessionId: String)(block: Request[AnyContent] => Result): Action[AnyContent]

  def defaultNotAuthorized(request: Request[AnyContent], code:Int, msg: String): Result = {
    import play.api.mvc.Results._
    Unauthorized(msg)
  }

  def createSession(id: String)(authorized: (Request[AnyContent]) => Result): Action[AnyContent] = createSessionHandleNotAuthorized(id)(authorized)(defaultNotAuthorized)

  /**
   * Optionally call create session and pass in a handler for not authorized
   */
  def createSessionHandleNotAuthorized(itemId: String)(authorized: (Request[AnyContent]) => Result)(failed: (Request[AnyContent], Int, String) => Result): Action[AnyContent]
}


