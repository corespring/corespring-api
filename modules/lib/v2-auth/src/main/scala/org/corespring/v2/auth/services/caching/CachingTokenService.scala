package org.corespring.v2.auth.services.caching

import org.corespring.models.Organization
import org.corespring.services.auth.AccessTokenService
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import play.api.Logger
import play.api.mvc.RequestHeader
import spray.caching.Cache

import scala.concurrent.Await
import scala.concurrent.duration._
import scalaz.{ Failure, Validation }

trait CachingTokenService extends AccessTokenService {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val logger = Logger(classOf[CachingTokenService])

  def underlying: AccessTokenService

  def timeToLiveInMinutes: Long

  private def timeToLive: Duration = timeToLiveInMinutes.minutes

  private val cache: Cache[Option[Organization]] = spray.caching.LruCache(timeToLive = timeToLive)

  override def orgForToken(token: String): Option[Organization] = {

    def callUnderlying = {
      logger.debug(s"token=$token - call underlying service")
      underlying.orgForToken(token)
    }

    Await.result(cache(token) { callUnderlying }, 5.seconds)
  }

  def flush = cache.clear()
}
