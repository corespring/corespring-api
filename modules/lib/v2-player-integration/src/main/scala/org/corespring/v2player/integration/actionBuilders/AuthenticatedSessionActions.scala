package org.corespring.v2player.integration.actionBuilders

import play.api.mvc._

import scalaz.Validation

trait AuthenticatedItem {
  def authenticationFailedResult(itemId: String, rh: RequestHeader): Option[SimpleResult]
}

trait SessionAuth {
  def read(sessionId: String)(implicit header: RequestHeader): Validation[String, Boolean]

  def createSession(itemId: String)(implicit header: RequestHeader): Validation[String, Boolean]

  def loadPlayerForSession(sessionId: String)(implicit header: RequestHeader): Validation[String, Boolean]
}

/*trait AuthenticatedSessionActions {
  def read(sessionId: String)(block: Request[AnyContent] => Result): Action[AnyContent]

  def defaultNotAuthorized(request: Request[AnyContent], code: Int, msg: String): Result = {
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

  def loadPlayerForSession(sessionId: String)(error: (Int, String) => Result)(block: (Request[AnyContent] => Result)): Action[AnyContent]
}
*/
