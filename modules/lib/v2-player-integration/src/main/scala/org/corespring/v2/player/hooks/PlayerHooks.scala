package org.corespring.v2.player.hooks

import org.bson.types.ObjectId
import org.corespring.container.client.hooks.{ PlayerHooks => ContainerPlayerHooks }
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.{ LoadOrgAndOptions, SessionAuth }
import org.corespring.v2.errors.Errors.{ cantParseItemId, errorSaving, generalError }
import org.corespring.v2.log.V2LoggerFactory
import play.api.http.Status._
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc._

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz._

trait PlayerHooks extends ContainerPlayerHooks with LoadOrgAndOptions {

  def sessionService: MongoService

  def itemService: ItemService

  def itemTransformer: ItemTransformer

  def auth: SessionAuth[OrgAndOpts]

  lazy val logger = V2LoggerFactory.getLogger("PlayerHooks")

  override def loadItem(sessionId: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {

    logger.trace(s"loadItem - sessionId: $sessionId")

    val s = for {
      identity <- getOrgIdAndOptions(header)
      models <- auth.loadForRead(sessionId)(identity)
    } yield models

    s.leftMap(s => UNAUTHORIZED -> s.message).rightMap { (models) =>
      val (_, item) = models
      val itemJson = itemTransformer.transformToV2Json(item)
      itemJson
    }.toEither
  }

  override def createSessionForItem(itemId: String)(implicit header: RequestHeader): Future[Either[(Int, String), String]] = Future {

    logger.trace(s"createSessionForItem: $itemId")

    def createSessionJson(vid: VersionedId[ObjectId]) = Json.obj(
      "_id" -> Json.obj(
        "$oid" -> ObjectId.get.toString),
      "itemId" -> vid.toString)

    val result = for {
      identity <- getOrgIdAndOptions(header)
      canWrite <- auth.canCreate(itemId)(identity)
      writeAllowed <- if (canWrite) Success(true) else Failure(generalError(s"Can't create session for $itemId"))
      vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
      item <- itemTransformer.updateV2Json(vid).toSuccess(generalError("Error generating item v2 JSON"))
      json <- Success(createSessionJson(vid))
      sessionId <- sessionService.create(json).toSuccess(errorSaving)
    } yield sessionId

    result
      .rightMap(oid => oid.toString)
      .leftMap(s => UNAUTHORIZED -> s.message)
      .toEither
  }

  override def loadPlayerForSession(sessionId: String)(implicit header: RequestHeader): Future[Option[(Int, String)]] = Future {
    logger.trace(s"loadPlayerForSession: sessionId")

    val out = for {
      identity <- getOrgIdAndOptions(header)
      id <- auth.loadForRead(sessionId)(identity)
    } yield id

    out match {
      case Failure(e) => {
        logger.trace(s"loadPlayerForSession failed: $sessionId: Error: $e")
        Some(UNAUTHORIZED -> e.message)
      }
      case _ => None
    }
  }

  override def loadSessionAndItem(sessionId: String)(implicit header: RequestHeader): Future[Either[(Int, String), (JsValue, JsValue)]] = Future {
    logger.trace(s"loadSessionAndItem: $sessionId")

    val o = for {
      identity <- getOrgIdAndOptions(header)
      models <- auth.loadForRead(sessionId)(identity)
    } yield models

    o.leftMap(s => UNAUTHORIZED -> s.message).rightMap { (models) =>
      val (session, item) = models
      (session, itemTransformer.transformToV2Json(item))
    }.toEither
  }
}

