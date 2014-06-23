package org.corespring.v2player.integration.hooks

import org.bson.types.ObjectId
import org.corespring.container.client.hooks.{ PlayerHooks => ContainerPlayerHooks }
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.auth.SessionAuth
import org.slf4j.LoggerFactory
import play.api.http.Status._
import play.api.libs.json.{ JsString, JsValue, Json }
import play.api.mvc._

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz._

trait PlayerHooks extends ContainerPlayerHooks {

  def sessionService: MongoService

  def itemService: ItemService

  def transformItem: Item => JsValue

  def auth: SessionAuth

  lazy val logger = LoggerFactory.getLogger("v2.integration.PlayerHooks")

  override def loadItem(sessionId: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
    val s: Validation[String, (JsValue, Item)] = for {
      models <- auth.loadForRead(sessionId)
    } yield models

    s.leftMap(s => UNAUTHORIZED -> s).rightMap { (models) =>
      val (_, item) = models
      val itemJson = transformItem(item)
      itemJson
    }.toEither
  }

  override def createSessionForItem(itemId: String)(implicit header: RequestHeader): Future[Either[(Int, String), String]] = Future {

    logger.trace(s"createSessionForItem: $itemId")

    def createSessionJson(vid: VersionedId[ObjectId]) = {
      Some(
        Json.obj(
          "_id" -> Json.obj("$oid" -> JsString(ObjectId.get.toString)),
          "itemId" -> JsString(vid.toString)))
    }

    val result = for {
      canWrite <- auth.canCreate(itemId)
      writeAllowed <- if (canWrite) Success(true) else Failure(s"Can't create session for $itemId")
      vid <- VersionedId(itemId).toSuccess(s"Error parsing item id: $itemId")
      json <- createSessionJson(vid).toSuccess("Error creating json")
      sessionId <- sessionService.create(json).toSuccess("Error creating session")
    } yield sessionId

    result
      .rightMap(oid => oid.toString)
      .leftMap(s => UNAUTHORIZED -> s)
      .toEither
  }

  override def loadPlayerForSession(sessionId: String)(implicit header: RequestHeader): Future[Option[(Int, String)]] = Future {
    auth.loadForRead(sessionId) match {
      case Failure(e) => Some(UNAUTHORIZED -> e)
      case _ => None
    }
  }

}

