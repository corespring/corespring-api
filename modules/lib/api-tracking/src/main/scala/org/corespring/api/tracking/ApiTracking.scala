package org.corespring.api.tracking

import akka.actor.{ ActorRef, Props }
import org.corespring.services.auth.{ AccessTokenService, ApiClientService }
import play.api.mvc.RequestHeader
import play.libs.Akka

trait ApiTracking {
  def handleRequest(r: RequestHeader): Unit
}

object NullTracking extends ApiTracking {
  override def handleRequest(r: RequestHeader): Unit = {}
}

class ApiTrackingLogger(
  tokenService: AccessTokenService,
  apiClientService: ApiClientService)
  extends ApiTracking {

  lazy val apiTracker: ActorRef = {
    val props = Props.create(
      classOf[ApiTrackingActor],
      new LoggingTrackingService(),
      tokenService,
      apiClientService).withDispatcher("akka.api-tracking-dispatcher")
    Akka.system.actorOf(props, "api-tracking")
  }

  def isLoggable(path: String): Boolean = {
    import org.corespring.container.client.controllers.apps.routes._
    val matchesPaths = Seq(
      Player.load(".*"),
      Player.createSessionForItem(".*"),
      DraftEditor.load(".*"),
      DraftDevEditor.load(".*"),
      ItemEditor.load(".*"),
      ItemDevEditor.load(".*")).map(_.url.r).exists { r => r.findFirstIn(path).isDefined }
    (path.contains("api") || matchesPaths)
  }

  override def handleRequest(r: RequestHeader): Unit = {
    if (isLoggable(r.path)) {
      apiTracker ! LogRequest(r)
    }
  }

}
