package org.corespring.v2player.integration.hooks

import org.bson.types.ObjectId
import org.corespring.container.client.hooks.{ PlayerHooks => ContainerPlayerHooks }
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.auth.SessionAuth
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

  private def versionedId(json: JsValue): Option[VersionedId[ObjectId]] = json match {
    case JsString(s) => VersionedId(s)
    case _ => None
  }

  override def loadItem(sessionId: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
    val s: Validation[String, (Item, JsValue)] = for {
      canRead <- auth.canRead(sessionId)
      readAllowed <- if (canRead) Success(true) else Failure("Can't read session")
      oid <- maybeOid(sessionId).toSuccess("Invalid object id")
      session <- sessionService.load(oid.toString).toSuccess("Session Not found")
      vId <- versionedId(session \ "itemId").toSuccess("Can't parse item id")
      item <- itemService.findOneById(vId).toSuccess("Can't find item")
    } yield (item, session)

    s.leftMap(s => UNAUTHORIZED -> s).rightMap { (models) =>
      val (item, _) = models
      val itemJson = transformItem(item)
      itemJson
    }.toEither
  }

  private def maybeOid(s: String): Option[ObjectId] = if (ObjectId.isValid(s)) Some(new ObjectId(s)) else None

  override def createSessionForItem(itemId: String)(implicit header: RequestHeader): Future[Either[(Int, String), String]] = Future {

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
    auth.canRead(sessionId) match {
      case Failure(e) => Some(UNAUTHORIZED -> e)
      case _ => None
    }
  }

}

