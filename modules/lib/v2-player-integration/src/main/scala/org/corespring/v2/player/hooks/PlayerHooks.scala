package org.corespring.v2.player.hooks

import org.bson.types.ObjectId
import org.corespring.platform.core.models.JsonUtil
import org.corespring.container.client.hooks.{ PlayerHooks => ContainerPlayerHooks }
import org.corespring.platform.core.models.item.{Item, PlayerDefinition}
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.{ LoadOrgAndOptions, SessionAuth }
import org.corespring.v2.errors.Errors.{ cantParseItemId, generalError }
import org.corespring.v2.log.V2LoggerFactory
import org.joda.time.{ DateTimeZone, DateTime }
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz._

trait PlayerHooks extends ContainerPlayerHooks with LoadOrgAndOptions with JsonUtil {

  def itemService: ItemService

  def itemTransformer: ItemTransformer

  def auth: SessionAuth[OrgAndOpts, PlayerDefinition]

  lazy val logger = V2LoggerFactory.getLogger("PlayerHooks")

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
}

