package common.controllers

import play.api.mvc.{ Action, Controller }
import scala.concurrent.ExecutionContext

object UserSession extends Controller {

  import ExecutionContext.Implicits.global

  val UserKey = "securesocial.user"
  val ProviderKey = "securesocial.provider"

  def logout = Action.async {
    request =>
      val loggedOutSession = request.session - UserKey - ProviderKey
      securesocial.controllers.LoginPage.logout(request).transform(r => r.withSession(loggedOutSession), e => e)
  }

}

