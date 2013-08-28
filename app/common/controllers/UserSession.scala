package common.controllers

import play.api.mvc.{ PlainResult, Session, Action, Controller }
import web.controllers.Main
import common.controllers.session.SessionHandler

class UserSession(handlers: SessionHandler*) extends Controller {

  def logout = Action {
    request =>
      val newSession = handlers.foldRight(request.session)((handler: SessionHandler, acc: Session) => handler.logout(acc))
      val result = securesocial.controllers.LoginPage.logout(request)
      if (result.isInstanceOf[PlainResult]) {
        result.asInstanceOf[PlainResult].withSession(newSession)
      } else {
        result
      }
  }
}

object UserSession extends UserSession(Main)
