package org.corespring.v2player.integration.actionBuilders

import play.api.mvc.{Action, Request, Result, AnyContent}

trait AuthenticatedSessionActions {
  def read(sessionId: String)(block: Request[AnyContent] => Result): Action[AnyContent]

  def defaultNotAuthorized(request: Request[AnyContent], code:Int, msg: String): Result = {
    import play.api.mvc.Results._
    Unauthorized(msg)
  }

  /**
   * The client wants to create a session for the given item id.
   * call authorized if authenticated
   * @param id
   * @param authorized
   * @return
   */
  def createSession(id: String)(authorized: (Request[AnyContent]) => Result): Action[AnyContent] = createSessionHandleNotAuthorized(id)(authorized)(defaultNotAuthorized)

  /**
   * The client wants to create a session for the given item id.
   * call authorized if successful, failed if not
   */
  def createSessionHandleNotAuthorized(itemId: String)(authorized: (Request[AnyContent]) => Result)(failed: (Request[AnyContent], Int, String) => Result): Action[AnyContent]


  def loadPlayerForSession(sessionId: String)(block : (Request[AnyContent] => Result)) : Action[AnyContent]
}


