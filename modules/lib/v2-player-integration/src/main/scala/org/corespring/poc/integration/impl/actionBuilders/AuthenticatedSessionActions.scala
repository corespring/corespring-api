package org.corespring.poc.integration.impl.actionBuilders

import org.corespring.poc.integration.impl.actionBuilders.access.{AccessResult, AccessDecider}
import play.api.mvc.Results._
import play.api.mvc.{Action, Request, Result, AnyContent}

trait AuthenticatedSessionActions {
  def read(id: String)(block: Request[AnyContent] => Result): Action[AnyContent]

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


class AuthenticatedSessionActionsImpl2(deciders: AccessDecider*) extends AuthenticatedSessionActions {

  def read(sessionId: String)(block: (Request[AnyContent]) => Result): Action[AnyContent] = Action {
    request =>

      val results: Seq[AccessResult] = deciders.map {
        d => d.accessForSessionId(sessionId, request)
      }

      if (results.exists(_.allowed)) {
        block(request)
      } else {
        Unauthorized(results.map(_.msgs).flatten.mkString("\n"))
      }
  }

  /**
   * Optionally call create session and pass in a handler for not authorized
   */
  def createSessionHandleNotAuthorized(itemId: String)(authorized: (Request[AnyContent]) => Result)(notAuthorized: (Request[AnyContent], String) => Result): Action[AnyContent] = Action {
    request =>

      val results: Seq[AccessResult] = deciders.map {
        d => d.accessForSessionId(itemId, request)
      }

      if (results.exists(_.allowed)) {
        authorized(request)
      } else {
        notAuthorized(request, results.map(_.msgs).flatten.mkString("\n"))
      }
  }
}
