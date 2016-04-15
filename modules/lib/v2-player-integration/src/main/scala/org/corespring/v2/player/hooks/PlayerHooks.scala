package org.corespring.v2.player.hooks

import org.bson.types.ObjectId
import org.corespring.container.client.hooks.{ PlayerHooks => ContainerPlayerHooks }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.models.appConfig.ArchiveConfig
import org.corespring.models.item.{ Item, PlayerDefinition }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.{ LoadOrgAndOptions, SessionAuth }
import org.corespring.v2.errors.Errors.{ cantParseItemId, generalError }
import org.corespring.v2.errors.V2Error
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz._

trait PlayerAssets {

  def loadItemFile(itemId: String, file: String)(implicit header: RequestHeader): SimpleResult

  def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult
}

trait PlayerItemProcessor {
  def makePlayerDefinitionJson(session: JsValue, playerDefinition: Option[PlayerDefinition]): JsValue
}

class PlayerHooks(
  archiveConfig: ArchiveConfig,
  getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts],
  itemService: ItemService,
  itemTransformer: ItemTransformer,
  playerAssets: PlayerAssets,
  playerItemProcessor: PlayerItemProcessor,
  sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition],
  override implicit val containerContext: ContainerExecutionContext)
  extends ContainerPlayerHooks with LoadOrgAndOptions {

  override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = getOrgAndOptsFn.apply(request)

  lazy val logger = Logger(classOf[PlayerHooks])

  override def createSessionForItem(itemId: String)(implicit header: RequestHeader): Future[Either[(Int, String), (JsValue, JsValue, JsValue)]] = Future {

    logger.debug(s"itemId=$itemId function=createSessionForItem")

    def createSessionJson(item: Item) = Json.obj(
      "itemId" -> item.id.toString,
      "collectionId" -> item.collectionId)

    lazy val getVid: Validation[V2Error, VersionedId[ObjectId]] = {
      val withVersion: Option[VersionedId[ObjectId]] = VersionedId(itemId).map {
        id =>
          lazy val currentVersion: Long = itemService.currentVersion(id)
          id.copy(version = id.version.orElse(Some(currentVersion)))
      }
      withVersion.toSuccess(cantParseItemId(itemId))
    }

    val result = for {
      identity <- getOrgAndOptions(header)
      canWrite <- sessionAuth.canCreate(itemId)(identity)
      writeAllowed <- if (canWrite) Success(true) else Failure(generalError(s"Can't create session for $itemId"))
      vid <- getVid
      item <- itemTransformer.loadItemAndUpdateV2(vid).toSuccess(generalError("Error generating item v2 JSON", INTERNAL_SERVER_ERROR))
      session <- Success(createSessionJson(item))
      sessionId <- sessionAuth.create(session)(identity)
      playerDefinitionJson <- Success(playerItemProcessor.makePlayerDefinitionJson(session, item.playerDefinition))
    } yield (Json.obj("id" -> sessionId.toString) ++ session, playerDefinitionJson, Json.obj())

    result
      .leftMap(s => UNAUTHORIZED -> s.message)
      .toEither
  }

  override def loadSessionAndItem(sessionId: String)(implicit header: RequestHeader): Future[Either[(Int, String), (JsValue, JsValue, JsValue)]] = Future {
    logger.debug(s"sessionId=$sessionId function=loadSessionAndItem")

    val o = for {
      identity <- getOrgAndOptions(header)
      models <- sessionAuth.loadForRead(sessionId)(identity)
    } yield models

    o.leftMap(s => s.statusCode -> s.message).rightMap { (models) =>
      val (session, playerDefinition) = models
      val playerDefinitionJson = playerItemProcessor.makePlayerDefinitionJson(session, Some(playerDefinition))
      val withId: JsValue = Json.obj("id" -> sessionId) ++ session.as[JsObject]
      (withId, playerDefinitionJson, Json.obj())
    }.toEither
  }

  override def loadItemFile(itemId: String, file: String)(implicit header: RequestHeader): SimpleResult = playerAssets.loadItemFile(itemId, file)(header)

  override def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult = playerAssets.loadFile(id, path)(request)

  override def archiveCollectionId: String = archiveConfig.contentCollectionId.toString()
}

