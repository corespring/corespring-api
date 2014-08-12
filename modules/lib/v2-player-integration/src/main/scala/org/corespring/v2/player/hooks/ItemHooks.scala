package org.corespring.v2.player.hooks

import org.bson.types.ObjectId
import org.corespring.container.client.hooks.Hooks.StatusMessage
import org.corespring.container.client.hooks.{ ItemHooks => ContainerItemHooks }
import org.corespring.platform.core.models.item.{ PlayerDefinition, Item => ModelItem }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.PlayerJsonToItem
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.{ ItemAuth, LoadOrgAndOptions }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.libs.json._
import play.api.mvc.RequestHeader

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

trait ItemHooks extends ContainerItemHooks with LoadOrgAndOptions {

  def transform: ModelItem => JsValue

  def auth: ItemAuth[OrgAndOpts]

  lazy val logger = V2LoggerFactory.getLogger("ItemHooks")

  override def load(itemId: String)(implicit header: RequestHeader): Future[Either[StatusMessage, JsValue]] = Future {
    val item: Validation[V2Error, JsValue] = for {
      identity <- getOrgIdAndOptions(header)
      vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
      item <- auth.loadForRead(itemId)(identity)
    } yield transform(item)

    item.leftMap(e => e.statusCode -> e.message).toEither
  }

  override def save(itemId: String, json: JsValue)(implicit header: RequestHeader): Future[Either[StatusMessage, JsValue]] = Future {

    logger.debug(s"save - itemId: $itemId")
    logger.trace(s"save - json: ${Json.stringify(json)}")

    /** an implementation for the container to save its definition */
    def convertAndSave(itemId: String, item: ModelItem, identity: OrgAndOpts): Option[JsValue] = {
      val updates = Seq(
        (item: ModelItem, json: JsValue) => PlayerJsonToItem.supportingMaterials(item, json),
        (item: ModelItem, json: JsValue) => (json \ "profile").asOpt[JsObject].map { obj => PlayerJsonToItem.profile(item, obj) }.getOrElse(item),
        (item: ModelItem, json: JsValue) => PlayerJsonToItem.playerDef(item, json))

      val updatedItem: ModelItem = updates.foldRight(item) { (fn, i) =>
        logger.trace(s"update item - fold")
        fn(i, json)
      }
      auth.save(updatedItem, createNewVersion = false)(identity)
      Some(transform(updatedItem))
    }

    val out: Validation[V2Error, JsValue] = for {
      identity <- getOrgIdAndOptions(header)
      vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
      item <- auth.loadForWrite(itemId)(identity)
      collectionId <- item.collectionId.toSuccess(noCollectionIdForItem(vid))
      result <- convertAndSave(itemId, item, identity).toSuccess(errorSaving())
    } yield {
      result
    }

    out.leftMap(e => e.statusCode -> e.message).toEither
  }

  override def create(maybeJson: Option[JsValue])(implicit header: RequestHeader): Future[Either[StatusMessage, String]] = Future {

    def createItem(collectionId: String, identity: OrgAndOpts): Option[VersionedId[ObjectId]] = {
      val definition = PlayerDefinition(Seq(), "<div>I'm a new item</div>", Json.obj(), "", None)
      val item = ModelItem(
        collectionId = Some(collectionId),
        playerDefinition = Some(definition))
      auth.insert(item)(identity)
    }

    val accessResult: Validation[V2Error, VersionedId[ObjectId]] = for {
      identity <- getOrgIdAndOptions(header)
      json <- maybeJson.toSuccess(noJson)
      collectionId <- (json \ "collectionId").asOpt[String].toSuccess(propertyNotFoundInJson("collectionId"))
      canWrite <- auth.canCreateInCollection(collectionId)(identity)
      hasAccess <- if (canWrite) {
        Success(true)
      } else {
        Failure(generalError("Write to item denied"))
      }
      id <- createItem(collectionId, identity).toSuccess(generalError("Error creating item"))
    } yield id

    accessResult.leftMap(e => e.statusCode -> e.message).rightMap(_.toString()).toEither

  }
}
