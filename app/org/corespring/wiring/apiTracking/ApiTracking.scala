package org.corespring.wiring.apiTracking

import akka.actor.{ ActorRef, Props }
import org.corespring.api.tracking.{ LogRequest, ApiCall, ApiTrackingActor, TrackingService }
import org.corespring.platform.core.models.auth.ApiClientService
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
    val v2PlayerRegex = org.corespring.container.client.controllers.apps.routes.Player.load(".*").url.r
    val v2EditorRegex = org.corespring.container.client.controllers.apps.routes.Editor.load(".*").url.r
    val isV2Player = v2PlayerRegex.findFirstIn(path).isDefined
    val isV2Editor = v2EditorRegex.findFirstIn(path).isDefined
    logRequests && (path.contains("api") || isV2Player || isV2Editor)
  }

  def handleRequest(r: RequestHeader): Unit = {
    if (logRequests && isLoggable(r.path)) {
      apiTracker ! LogRequest(r)
    }
  }

}
