package org.corespring.poc.integration.impl.actionBuilders

import play.api.mvc.{Action, Request, Result, AnyContent}

trait AuthenticatedSessionActions {
  def read(sessionId: String)(block: Request[AnyContent] => Result): Action[AnyContent]

  def defaultNotAuthorized(request: Request[AnyContent], msg: String): Result = {
    import play.api.mvc.Results._
    Unauthorized(msg)
  }

  def createSession(id: String)(authorized: (Request[AnyContent]) => Result): Action[AnyContent] = createSessionHandleNotAuthorized(id)(authorized)(defaultNotAuthorized)

  /**
   * Optionally call create session and pass in a handler for not authorized
   */
  def createSessionHandleNotAuthorized(id: String)(authorized: (Request[AnyContent]) => Result)(notAuthorized: (Request[AnyContent], String) => Result): Action[AnyContent]
}


