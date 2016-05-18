package org.corespring.v2.auth.wired

import com.mongodb.casbah.Imports._
import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.errors.PlatformServiceError
import org.corespring.models.item.PlayerDefinition
import org.corespring.models.json.JsonFormatting
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.PlayerDefinitionService
import org.corespring.v2.auth.{ItemAuth, PlayerDefinitionLoader}
import org.corespring.v2.auth.models.{OrgAndOpts, PlayerAccessSettings}
import org.corespring.v2.errors.Errors.noItemIdInSession
import org.corespring.v2.errors.V2Error
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue}
import spray.caching.Cache

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scalaz.{Failure, Success, Validation}

trait HasPermissions {
  def has(itemId: String, sessionId: Option[String], settings: PlayerAccessSettings): Validation[V2Error, Boolean]
}

class CachingPlayerDefinitionService(underlying: PlayerDefinitionService) extends PlayerDefinitionService{
  private lazy val logger = Logger(this.getClass)

  private val timeToLive : Duration = 1.minute

  private val cache : Cache[Validation[PlatformServiceError,PlayerDefinition]] = spray.caching.LruCache(timeToLive = timeToLive)

  override def findMultiplePlayerDefinitions(orgId: ObjectId, ids: VersionedId[ObjectId]*): Future[Seq[(VersionedId[ObjectId], Validation[PlatformServiceError, PlayerDefinition])]] = {
    logger.debug(s"function=findMultiplePlayerDefinitions")

    val cachedResults = ids.map{ id => id -> cache.get(id) }
    val (missing, cached) = cachedResults.partition(_._2.isEmpty)
    val missingIds = missing.map(_._1)

    for{
        
    }
    underlying.findMultiplePlayerDefinitions(orgId, missingIds : _*).map{ results =>

      Future.sequence(cached.map(_._2).flatten).flatMap{ cached =>
        results ++ cached
      }
    }
  }
}


class PlayerDefinitionLoaderWired(
  itemTransformer: ItemTransformer,
  jsonToPlayerDef: JsonFormatting,
  itemAuth: ItemAuth[OrgAndOpts],
  perms: HasPermissions) extends PlayerDefinitionLoader {

  lazy val logger = Logger(classOf[PlayerDefinitionLoaderWired])

  implicit def ec: ExecutionContext = ExecutionContext.global

  def timeToLiveInMinutes: Long = 60

  private def timeToLive: Duration = timeToLiveInMinutes.minutes

  private val cache: Cache[Validation[V2Error, PlayerDefinition]] = spray.caching.LruCache(timeToLive = timeToLive)

  def loadPlayerDefinition(sessionId: String, session: JsValue)(implicit identity: OrgAndOpts): Validation[V2Error, PlayerDefinition] = {

    def getPlayerDefinition(itemId: String): Validation[V2Error, PlayerDefinition] = {
      for {
        item <- itemAuth.loadForRead(itemId)
        hasPerms <- perms.has(item.id.toString, Some(sessionId), identity.opts)
        pd <- Success(itemTransformer.createPlayerDefinition(item))
      } yield pd
    }

    val sessionPlayerDef: Option[PlayerDefinition] = (session \ "item").asOpt[JsObject].map {
      internalModel =>
        jsonToPlayerDef.toPlayerDefinition(internalModel)
    }.flatten

    sessionPlayerDef
      .map { d => Success(d) }
      .getOrElse {
        (session \ "itemId").asOpt[String] match {
          case None => Failure(noItemIdInSession(sessionId))
          case Some(itemId) =>
            Await.result(cache(itemId) {
              getPlayerDefinition(itemId)
            }, 5.seconds)
        }
      }
  }

  def loadMultiplePlayerDefinitions(sessions: Seq[(String, Validation[V2Error, JsValue])])(implicit identity: OrgAndOpts): Seq[(String, Validation[V2Error, (JsValue, PlayerDefinition)])] = {
    sessions.map {
      case (id:String, Success(json)) => loadPlayerDefinition(id, json) match {
        case Success(pd) => (id, Success((json, pd)))
        case Failure(err) => (id, Failure(err))
      }
      case (id:String, Failure(err)) => (id, Failure(err))
    }
  }
}
