package org.corespring.v2.api.services

import com.mongodb.casbah.Imports._
import org.corespring.errors.PlatformServiceError
import org.corespring.models.item.PlayerDefinition
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.PlayerDefinitionService
import play.api.Logger
import spray.caching.Cache

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.Validation

private[api] object CachingPlayerDefinitionService {
  type CacheType = Validation[PlatformServiceError, PlayerDefinition]
  type Vid = VersionedId[ObjectId]
}

import org.corespring.v2.api.services.CachingPlayerDefinitionService._

class CachingPlayerDefinitionService(
  underlying: PlayerDefinitionService,
  cache: Cache[CacheType])(
    implicit val ec: ExecutionContext) extends PlayerDefinitionService {

  private lazy val logger = Logger(this.getClass)

  override def findMultiplePlayerDefinitions(orgId: ObjectId, ids: Vid*): Future[Seq[(Vid, CacheType)]] = {
    logger.debug(s"function=findMultiplePlayerDefinitions, ids.length=${ids.length}")

    val cachedResults = ids.map { id => id -> cache.get(id) }
    val (missing, cached) = cachedResults.partition(_._2.isEmpty)
    val missingIds = missing.map(_._1)

    val futureCachedResults = Future.sequence(cached.flatMap {
      case ((id, Some(f))) => Some(f.map(id -> _))
      case _ => None
    })

    val futureUnderlyingResults = underlying.findMultiplePlayerDefinitions(orgId, missingIds: _*).map { r =>
      r.foreach {
        case ((id, v)) => {
          logger.trace(s"function=findMultiplePlayerDefinition, id=$id - cache the result")
          cache(id, () => Future.successful(v))
        }
      }
      r
    }

    for {
      fcr <- futureCachedResults
      ur <- futureUnderlyingResults
    } yield {
      fcr ++ ur
    }
  }

}
