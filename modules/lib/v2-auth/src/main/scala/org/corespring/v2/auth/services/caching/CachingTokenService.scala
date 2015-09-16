package org.corespring.v2.auth.services.caching

import org.corespring.models.Organization
import org.corespring.services.auth.AccessTokenService
import org.corespring.services.errors.PlatformServiceError
import play.api.Logger
import spray.caching.Cache

import scala.concurrent.{ ExecutionContext, Await }
import scala.concurrent.duration._
import scalaz.Validation

trait CachingTokenService extends AccessTokenService {

  implicit def ec: ExecutionContext

  private val logger = Logger(classOf[CachingTokenService])

  def underlying: AccessTokenService

  def timeToLiveInMinutes: Long

  private def timeToLive: Duration = timeToLiveInMinutes.minutes

  private val cache: Cache[Validation[PlatformServiceError, Organization]] = spray.caching.LruCache(timeToLive = timeToLive)

  override def orgForToken(token: String): Validation[PlatformServiceError, Organization] = {

    def callUnderlying = {
      logger.debug(s"token=$token - call underlying service")
      underlying.orgForToken(token)
    }

    Await.result(cache(token) { callUnderlying }, 5.seconds)
  }

  def flush = cache.clear()
}
