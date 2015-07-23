package common.controllers

import common.controllers.session.SessionHandler
import play.api.mvc.{ Session, Action, Controller }
import web.controllers.Main
import scala.concurrent.ExecutionContext

class UserSession(handlers: SessionHandler*) extends Controller {

  import ExecutionContext.Implicits.global

  def logout = Action.async {
    request =>
      val newSession = handlers.foldRight(request.session)((handler: SessionHandler, acc: Session) => handler.logout(acc))
      securesocial.controllers.LoginPage.logout(request).transform(r => r.withSession(newSession), e => e)
  }
}

object UserSession extends UserSession(Main)
