package org.corespring.api.v2.actions

import org.corespring.api.v2.services.{ OrgService, TokenService }
import org.corespring.platform.core.controllers.auth.TokenReader
import play.api.mvc._
import scala.concurrent.Future

trait TokenAuthenticated
  extends V2ApiActions[AnyContent]
  with Controller
  with TokenReader {

  def tokenService: TokenService
  def orgService: OrgService


  private def failed(s:String) : Future[SimpleResult] = Future(Unauthorized(s))

  private def orgAction(failed: String => Future[SimpleResult],
                 block: OrgRequest[AnyContent] => Future[SimpleResult]) : Action[AnyContent] = Action.async { r: Request[AnyContent] =>
    def onToken(token: String) = {
      val result = for {
        org <- tokenService.orgForToken(token)
        dc <- orgService.defaultCollection(org)
      } yield {
        block(OrgRequest(r, org.id, dc))
      }
      result.getOrElse(failed(""))
    }

    def onError(msg: String) = failed(msg)
    getToken[String](r, "Invalid token", "No token").fold(onError, onToken)
  }

  override def OrgAction( block: OrgRequest[AnyContent] => Future[SimpleResult]): Action[AnyContent] =  orgAction(failed, block)
}
