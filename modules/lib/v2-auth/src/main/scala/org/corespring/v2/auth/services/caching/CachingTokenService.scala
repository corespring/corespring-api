package org.corespring.v2.auth.services.caching

import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.services.TokenService
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.mvc.RequestHeader
import spray.caching.Cache

import scala.concurrent.{Future, ExecutionContext, Await}
import scala.concurrent.duration._
import scalaz.{Success, Validation}

trait CachingTokenService extends TokenService{


  import ExecutionContext.Implicits.global

  private val logger = V2LoggerFactory.getLogger("CachingTokenService")

  def underlying : TokenService

  def timeToLiveInMinutes : Long

  private def timeToLive : Duration = timeToLiveInMinutes.minutes

  private val cache: Cache[Organization] = spray.caching.LruCache(timeToLive = timeToLive)

  override def orgForToken(token: String)(implicit rh: RequestHeader): Validation[V2Error, Organization] = {

    cache.get(token) match {
      case Some(futureOrg) => {
        logger.debug(s"found org in cache for token: $token")
        Await.result(futureOrg.map(Success(_)), 5.seconds)
      }
      case _ => {
        val out = underlying.orgForToken(token)
        out match {
          case Success(o) => {
            logger.debug(s"found org in underlying for token $token - putting into cache")
            cache.apply(token, () => Future{o})
          }
          case _ => //do nothing
        }
        out
      }
    }
  }
}
