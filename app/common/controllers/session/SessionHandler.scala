package common.controllers.session

import play.api.mvc.Session

trait SessionHandler {
  def logout(session: Session): Session
}
