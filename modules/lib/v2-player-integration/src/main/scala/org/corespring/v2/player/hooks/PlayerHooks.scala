package org.corespring.v2.player.hooks

import org.bson.types.ObjectId
import org.corespring.platform.core.models.JsonUtil
import org.corespring.container.client.hooks.{ PlayerHooks => ContainerPlayerHooks }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.models.item.PlayerDefinition
import org.corespring.models.json.JsonFormatting
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.{ LoadOrgAndOptions, SessionAuth }
import org.corespring.v2.errors.Errors.{ cantParseItemId, generalError }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.V2PlayerExecutionContext
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.Scalaz._
import scalaz._

trait PlayerAssets {

  def loadItemFile(itemId: String, file: String)(implicit header: RequestHeader): SimpleResult

  def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult
}

class PlayerHooks(
  itemService: ItemService,
  itemTransformer: ItemTransformer,
  auth: SessionAuth[OrgAndOpts, PlayerDefinition],
  jsonFormatting: JsonFormatting,
  playerAssets: PlayerAssets,
  getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts],
  override implicit val containerContext: ContainerExecutionContext) extends ContainerPlayerHooks with LoadOrgAndOptions {

  implicit val formatPlayerDefinition = jsonFormatting.formatPlayerDefinition

  override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = getOrgAndOptsFn.apply(request)

  lazy val logger = Logger(classOf[PlayerHooks])

  override def createSessionForItem(itemId: String)(implicit header: RequestHeader): Future[Either[(Int, String), (JsValue, JsValue)]] = Future {

    logger.debug(s"itemId=$itemId function=createSessionForItem")

    def createSessionJson(item: Item) = partialObj("itemId" -> Some(JsString(item.id.toString)),
      "collectionId" -> item.collectionId.map(JsString(_)))

    val result = for {
      identity <- getOrgAndOptions(header)
      canWrite <- auth.canCreate(itemId)(identity)
      writeAllowed <- if (canWrite) Success(true) else Failure(generalError(s"Can't create session for $itemId"))
      vid <- VersionedId(itemId).map(id => id.version match {
        case Some(version) => id
        case None => id.copy(version = Some(itemService.currentVersion(id)))
      }).toSuccess(cantParseItemId(itemId))
      item <- itemTransformer.loadItemAndUpdateV2(vid).toSuccess(generalError("Error generating item v2 JSON", INTERNAL_SERVER_ERROR))
      json <- Success(createSessionJson(item))
      sessionId <- auth.create(json)(identity)
    } yield (Json.obj("id" -> sessionId.toString) ++ json, Json.toJson(item.playerDefinition))

    result
      .leftMap(s => UNAUTHORIZED -> s.message)
      .toEither
  }

  override def loadSessionAndItem(sessionId: String)(implicit header: RequestHeader): Future[Either[(Int, String), (JsValue, JsValue)]] = Future {
    logger.debug(s"sessionId=$sessionId function=loadSessionAndItem")

    val o = for {
      identity <- getOrgAndOptions(header)
      models <- auth.loadForRead(sessionId)(identity)
    } yield models

    o.leftMap(s => s.statusCode -> s.message).rightMap { (models) =>
      val (session, playerDefinition) = models
      val v2Json = Json.toJson(playerDefinition) //itemTransformer.transformToV2Json(item)

      //Ensure that only the requested properties are returned
      val playerV2Json = Json.obj(
        "xhtml" -> (v2Json \ "xhtml").as[String],
        "components" -> (v2Json \ "components").as[JsValue],
        "summaryFeedback" -> (v2Json \ "summaryFeedback").as[String])

      val withId: JsValue = Json.obj("id" -> sessionId) ++ session.as[JsObject]
      (withId, playerV2Json)
    }.toEither
  }

  override def loadItemFile(itemId: String, file: String)(implicit header: RequestHeader): SimpleResult = playerAssets.loadItemFile(itemId, file)(header)

  override def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult = playerAssets.loadFile(id, path)(request)
}

