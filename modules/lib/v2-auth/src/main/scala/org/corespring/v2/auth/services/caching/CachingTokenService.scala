package org.corespring.v2.auth.services.caching

import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.services.TokenService
import org.corespring.v2.errors.V2Error
import play.api.mvc.RequestHeader
import spray.caching.Cache

import scala.concurrent.{Future, ExecutionContext, Await}
import scala.concurrent.duration._
import scalaz.Validation

trait CachingTokenService extends TokenService{

  import ExecutionContext.Implicits.global

  def underlying : TokenService

  def timeToLiveInMinutes : Long

  private def timeToLive : Duration = timeToLiveInMinutes.minutes

  private val cache: Cache[Validation[V2Error,Organization]] = spray.caching.LruCache(timeToLive = timeToLive)

  override def orgForToken(token: String)(implicit rh: RequestHeader): Validation[V2Error, Organization] = {
    Await.result(cache(token,  () => Future{underlying.orgForToken(token)} ), 5.seconds)
  }
}
