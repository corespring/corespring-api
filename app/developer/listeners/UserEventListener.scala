package developer.listeners

import org.corespring.legacy.ServiceLookup
import play.Application
import play.api.mvc.{ Session, RequestHeader }
import securesocial.core.{ LoginEvent, SignUpEvent, Event, EventListener }

/**
 * EventListener for user updates. Presently it simply updates user timestamps.
 */
class UserEventListener(app: Application) extends EventListener {

  override def id: String = "corespring_user_event_listener"

  def onEvent(event: Event, request: RequestHeader, session: Session): Option[Session] = {
    event match {
      case _: LoginEvent => ServiceLookup.userService.touchLastLogin(event.user.identityId.userId)
      case _: SignUpEvent => ServiceLookup.userService.touchRegistration(event.user.identityId.userId)
      case _ =>
    }
    Some(session)
  }
}
