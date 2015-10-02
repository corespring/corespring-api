package org.corespring.api.tracking

import akka.actor.{ ActorRef, Props }
import org.corespring.services.auth.{ AccessTokenService, ApiClientService }
import play.api.Mode.Mode
import play.api.mvc.RequestHeader
import play.api.{ Configuration, Logger, Mode }
import play.libs.Akka

class ApiTracking(tokenService: AccessTokenService, apiClientService: ApiClientService)(config: Configuration, appMode: Mode) {

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
    val out = config.getBoolean("api.log-requests").getOrElse(appMode == Mode.Dev)
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