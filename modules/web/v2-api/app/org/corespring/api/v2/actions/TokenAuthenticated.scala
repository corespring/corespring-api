package org.corespring.api.v2.actions

import org.corespring.api.v2.services.{ OrgService, TokenService }
import org.corespring.platform.core.controllers.auth.TokenReader
import play.api.mvc._
import scala.concurrent.{ ExecutionContext, Future }

trait TokenAuthenticated[A]
  extends V2ApiActions[A]
  with Controller
  with TokenReader {

  def tokenService: TokenService
  def orgService: OrgService

  implicit def ec: ExecutionContext

  private def failed(s: String): Future[SimpleResult] = Future(Unauthorized(s))

  private def orgAction(failed: String => Future[SimpleResult],
    block: OrgRequest[A] => Future[SimpleResult], p: BodyParser[A]): Action[A] = Action.async(p) { r: Request[A] =>
    def onToken(token: String) = {
      val result = for {
        org <- tokenService.orgForToken(token)
        dc <- orgService.defaultCollection(org)
      } yield {
        block(OrgRequest[A](r, org.id, dc))
      }
      result.getOrElse(failed(""))
    }

    def onError(msg: String) = failed(msg)
    getToken[String](r, "Invalid token", "No token").fold(onError, onToken)
  }

  override def orgAction(bp: BodyParser[A])(block: OrgRequest[A] => Future[SimpleResult]): Action[A] = orgAction(failed, block, bp)
}
