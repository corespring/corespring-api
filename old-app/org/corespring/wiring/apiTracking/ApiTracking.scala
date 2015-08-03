package org.corespring.wiring.apiTracking

import akka.actor.{ ActorRef, Props }
import org.corespring.api.tracking.{ LogRequest, ApiCall, ApiTrackingActor, TrackingService }
import org.corespring.models.auth.ApiClientService
import org.corespring.v2.auth.services.TokenService
import play.api.mvc.RequestHeader
import play.api.{ Logger, Mode, Play }
import play.libs.Akka

class ApiTracking(tokenService: TokenService, apiClientService: ApiClientService) {

  private lazy val logger = Logger("org.corespring.ApiTrackingWiring")

  lazy val trackingService = new TrackingService {

    private val logger = Logger("org.corespring.api.tracking")

    override def log(c: => ApiCall): Unit = {
      logger.info(c.toKeyValues)
    }
  }

  lazy val apiTracker: ActorRef = Akka.system.actorOf(
    Props.create(classOf[ApiTrackingActor], trackingService, tokenService, apiClientService)
      .withDispatcher("akka.api-tracking-dispatcher"), "api-tracking")

  lazy val logRequests = {
    val out = Play.current.configuration.getBoolean("api.log-requests").getOrElse(Play.current.mode == Mode.Dev)
    logger.info(s"Log api requests? ${out}")
    out
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
    logRequests && (path.contains("api") || matchesPaths)
  }

  def handleRequest(r: RequestHeader): Unit = {
    if (logRequests && isLoggable(r.path)) {
      apiTracker ! LogRequest(r)
    }
  }

}
