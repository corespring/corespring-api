package org.corespring.wiring.apiTracking

import akka.actor.{ Props, ActorRef }
import org.corespring.api.tracking.{ ApiCall, TrackingService, ApiTrackingActor }
import org.corespring.platform.core.caching.SimpleCache
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.{ ApiClient, ApiClientService }
import org.corespring.v2.auth.services.TokenService
import org.corespring.v2.errors.V2Error
import org.corespring.wiring.AppWiring
import play.api.{ Logger, Mode, Play }
import play.api.mvc.RequestHeader
import play.libs.Akka

import scalaz.Validation

object ApiTrackingWiring {

  private lazy val logger = Logger("org.corespring.ApiTrackingWiring")

  lazy val trackingService = new TrackingService {

    private val logger = Logger("org.corespring.api.tracking")

    override def log(c: => ApiCall): Unit = {
      logger.info(c.toKeyValues)
    }
  }

  lazy val apiTracker: ActorRef = Akka.system.actorOf(
    Props.create(classOf[ApiTrackingActor], trackingService, cachingTokenService, cachingApiClientService)
      .withDispatcher("akka.api-tracking-dispatcher"), "api-tracking")

  lazy val logRequests = {
    val out = Play.current.configuration.getBoolean("api.log-requests").getOrElse(Play.current.mode == Mode.Dev)
    logger.info(s"Log api requests? ${out}")
    out
  }

  def isLoggable(path: String): Boolean = {
    val v2PlayerRegex = org.corespring.container.client.controllers.apps.routes.ProdHtmlPlayer.config(".*").url.r
    val v2EditorRegex = org.corespring.container.client.controllers.apps.routes.Editor.editItem(".*").url.r
    val isV2Player = v2PlayerRegex.findFirstIn(path).isDefined
    val isV2Editor = v2EditorRegex.findFirstIn(path).isDefined
    logRequests && (path.contains("api") || isV2Player || isV2Editor)
  }

  lazy val cachingApiClientService = new ApiClientService {

    val localCache = new SimpleCache[ApiClient] {
      override def timeToLiveInMinutes = Play.current.configuration.getDouble("api.cache.ttl-in-minutes").getOrElse(3)
    }

    override def findByKey(key: String): Option[ApiClient] = localCache.get(key).orElse {
      val out = ApiClient.findByKey(key)
      out.foreach(localCache.set(key, _))
      out
    }
  }

  lazy val cachingTokenService = new TokenService {
    val localCache = new SimpleCache[Validation[V2Error, Organization]] {
      override def timeToLiveInMinutes = Play.current.configuration.getDouble("api.cache.ttl-in-minutes").getOrElse(3)
    }

    override def orgForToken(token: String)(implicit rh: RequestHeader): Validation[V2Error, Organization] =
      localCache.get(token).getOrElse {
        val out = AppWiring.integration.tokenService.orgForToken(token)(rh)
        localCache.set(token, out)
        out
      }
  }

}
