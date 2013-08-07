package plugins

import org.corespring.platform.core.models.User
import play.Application
import play.api.mvc.{Session, RequestHeader}
import securesocial.core.{LoginEvent, SignUpEvent, Event, EventListener}

/**
 * EventListener for user updates. Presently it simply updates user timestamps.
 */
class UserEventListener(app: Application) extends EventListener {

  override def id: String = "corespring_user_event_listener"

  def onEvent(event: Event, request: RequestHeader, session: Session): Option[Session] = {
    event match {
      case _: LoginEvent => User.touchLastLogin(event.user.id)
      case _: SignUpEvent => User.touchRegistration(event.user.id)
      case _ => {}
    }
    Some(session)
  }
}
