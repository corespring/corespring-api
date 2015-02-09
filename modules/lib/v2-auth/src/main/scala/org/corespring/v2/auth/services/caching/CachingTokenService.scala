package org.corespring.v2.auth.services.caching

import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.v2.auth.services.TokenService
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.mvc.RequestHeader
import spray.caching.Cache

import scala.concurrent.Await
import scala.concurrent.duration._
import scalaz.Validation

trait CachingTokenService extends TokenService {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val logger = V2LoggerFactory.getLogger("CachingTokenService")

  def underlying: TokenService

  def timeToLiveInMinutes: Long

  private def timeToLive: Duration = timeToLiveInMinutes.minutes

  private val cache: Cache[Validation[V2Error, Organization]] = spray.caching.LruCache(timeToLive = timeToLive)
  private val clientCache: Cache[Validation[V2Error, ApiClient]] = spray.caching.LruCache(timeToLive = timeToLive)

  override def orgForToken(token: String)(implicit rh: RequestHeader): Validation[V2Error, Organization] = {

    def callUnderlying = {
      logger.debug(s"token=$token - call underlying service")
      underlying.orgForToken(token)
    }

    Await.result(cache(token) { callUnderlying }, 5.seconds)
  }

  override def apiClientForToken(token: String)(implicit rh: RequestHeader): Validation[V2Error, ApiClient] = {
    def callUnderlying = {
      logger.debug(s"token=$token - call underlying service")
      underlying.apiClientForToken(token)
    }

    Await.result(clientCache(token) { callUnderlying }, 5.seconds)
  }

  def flush = cache.clear()
}
