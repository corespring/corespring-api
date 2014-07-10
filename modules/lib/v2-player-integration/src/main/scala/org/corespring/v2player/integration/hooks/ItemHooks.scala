package org.corespring.v2player.integration.hooks

import org.corespring.v2.auth.ItemAuth

import scala.concurrent.Future

import org.bson.types.ObjectId
import org.corespring.container.client.hooks.{ ItemHooks => ContainerItemHooks }
import org.corespring.container.client.hooks.Hooks.StatusMessage
import org.corespring.platform.core.models.item.{ Item => ModelItem, PlayerDefinition }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.errors.V2Error
import org.corespring.v2player.integration.errors.Errors._
import org.corespring.v2player.integration.transformers.container.PlayerJsonToItem
import org.slf4j.LoggerFactory
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import scalaz.{ Failure, Success, Validation }
import scalaz.Scalaz._

trait ItemHooks extends ContainerItemHooks {

  def transform: ModelItem => JsValue

  def auth: ItemAuth

  lazy val logger = LoggerFactory.getLogger("v2.integration.hooks.item")

  override def load(itemId: String)(implicit header: RequestHeader): Future[Either[StatusMessage, JsValue]] = Future {
    val item: Validation[V2Error, JsValue] = for {
      vid <- VersionedId(itemId).toSuccess(cantParseItemId)
      item <- auth.loadForRead(itemId).leftMap(generalError(UNAUTHORIZED, _))
    } yield transform(item)

    item.leftMap(e => e.code -> e.message).toEither
  }

  override def save(itemId: String, json: JsValue)(implicit header: RequestHeader): Future[Either[StatusMessage, JsValue]] = Future {

    logger.debug(s"save - itemId: $itemId")
    logger.trace(s"save - json: ${Json.stringify(json)}")

    /** an implementation for the container to save its definition */
    def convertAndSave(itemId: String, item: ModelItem): Option[JsValue] = {
      val updates = Seq(
        (item: ModelItem, json: JsValue) => PlayerJsonToItem.supportingMaterials(item, json),
        (item: ModelItem, json: JsValue) => (json \ "profile").asOpt[JsObject].map { obj => PlayerJsonToItem.profile(item, obj) }.getOrElse(item),
        (item: ModelItem, json: JsValue) => PlayerJsonToItem.playerDef(item, json))

      val updatedItem: ModelItem = updates.foldRight(item) { (fn, i) =>
        logger.trace(s"update item - fold")
        fn(i, json)
      }
      auth.save(updatedItem, false)
      Some(transform(updatedItem))
    }

    val out: Validation[V2Error, JsValue] = for {
      vid <- VersionedId(itemId).toSuccess(cantParseItemId)
      item <- auth.loadForWrite(itemId).leftMap(generalError(UNAUTHORIZED, _))
      collectionId <- item.collectionId.toSuccess(noCollectionIdForItem(vid))
      result <- convertAndSave(itemId, item).toSuccess(errorSaving)
    } yield {
      result
    }

    out.leftMap(e => e.code -> e.message).toEither
  }

  override def create(maybeJson: Option[JsValue])(implicit header: RequestHeader): Future[Either[StatusMessage, String]] = Future {

    def createItem(collectionId: String): Option[VersionedId[ObjectId]] = {
      val definition = PlayerDefinition(Seq(), "<div>I'm a new item</div>", Json.obj(), "")
      val item = ModelItem(
        collectionId = Some(collectionId),
        playerDefinition = Some(definition))
      auth.insert(item)
    }

    val accessResult: Validation[V2Error, VersionedId[ObjectId]] = for {
      json <- maybeJson.toSuccess(noJson)
      collectionId <- (json \ "collectionId").asOpt[String].toSuccess(propertyNotFoundInJson("collectionId"))
      canWrite <- auth.canCreateInCollection(collectionId).leftMap(generalError(UNAUTHORIZED, _))
      hasAccess <- if (canWrite) {
        Success(true)
      } else {
        Failure(generalError(UNAUTHORIZED, "?"))
      }
      id <- createItem(collectionId).toSuccess(generalError(INTERNAL_SERVER_ERROR, "Error creating item"))
    } yield id

    accessResult.leftMap(e => e.code -> e.message).rightMap(_.toString).toEither

  }
}
