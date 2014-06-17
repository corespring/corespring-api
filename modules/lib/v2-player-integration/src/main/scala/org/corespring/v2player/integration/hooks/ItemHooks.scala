package org.corespring.v2player.integration.hooks

import org.bson.types.ObjectId
import org.corespring.container.client.actions.{ HttpStatusMessage, ItemHooks => ContainerItemHooks }
import org.corespring.platform.core.models.item.{ Item, PlayerDefinition, Item => ModelItem }
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.auth.ItemAuth
import org.corespring.v2player.integration.errors.Errors._
import org.corespring.v2player.integration.errors.V2Error
import org.corespring.v2player.integration.transformers.container.PlayerJsonToItem
import org.slf4j.LoggerFactory
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

trait ItemHooks extends ContainerItemHooks {

  def itemService: ItemService

  def orgService: OrganizationService

  def transform: ModelItem => JsValue

  def auth: ItemAuth

  lazy val logger = LoggerFactory.getLogger("v2.integration.hooks.item")

  override def load(itemId: String)(implicit header: RequestHeader): Future[Either[HttpStatusMessage, JsValue]] = Future {
    val item: Validation[V2Error, JsValue] = for {
      vid <- VersionedId(itemId).toSuccess(cantParseItemId)
      canAccess <- auth.canAccessItem(itemId).leftMap(generalError(UNAUTHORIZED, _))
      item <- itemService.findOneById(vid).toSuccess(cantFindItemWithId(vid))
      hasAccess <- if (canAccess) {
        Success(item)
      } else {
        Failure(generalError(UNAUTHORIZED, "?"))
      }
    } yield transform(item)

    item.leftMap(e => HttpStatusMessage(e.code, e.message)).toEither
  }

  override def save(itemId: String, json: JsValue)(implicit header: RequestHeader): Future[Either[HttpStatusMessage, JsValue]] = Future {

    logger.debug(s"save - itemId: $itemId")
    logger.trace(s"save - json: ${Json.stringify(json)}")

    /** an implementation for the container to save its definition */
    def convertAndSave(itemId: String, item: ModelItem): Option[JsValue] = {
      val updates = Seq(
        (item: ModelItem, json: JsValue) => PlayerJsonToItem.supportingMaterials(item, json),
        (item: ModelItem, json: JsValue) => (json \ "profile").asOpt[JsObject].map { obj => PlayerJsonToItem.profile(item, obj) }.getOrElse(item),
        (item: ModelItem, json: JsValue) => PlayerJsonToItem.playerDef(item, json))

      val updatedItem: ModelItem = updates.foldRight(item) { (fn, i) => fn(i, json) }
      itemService.save(updatedItem, false)
      Some(transform(updatedItem))
    }

    val out: Validation[V2Error, JsValue] = for {
      vid <- VersionedId(itemId).toSuccess(cantParseItemId)
      canWrite <- auth.canWriteItem(itemId).leftMap(generalError(UNAUTHORIZED, _))
      hasAccess <- if (canWrite) {
        Success(true)
      } else {
        Failure(generalError(UNAUTHORIZED, "?"))
      }
      item <- itemService.findOneById(vid).toSuccess(cantFindItemWithId(vid))
      result <- convertAndSave(itemId, item).toSuccess(errorSaving)
    } yield {
      result
    }

    out.leftMap(e => HttpStatusMessage(e.code, e.message)).toEither
  }

  override def create(maybeJson: Option[JsValue])(implicit header: RequestHeader): Future[Either[HttpStatusMessage, String]] = Future {

    def createItem(collectionId: String): Option[VersionedId[ObjectId]] = {
      val definition = PlayerDefinition(Seq(), "<div>I'm a new item</div>", Json.obj(), "")
      val item = Item(
        collectionId = Some(collectionId),
        playerDefinition = Some(definition))
      itemService.insert(item)
    }

    val accessResult: Validation[V2Error, VersionedId[ObjectId]] = for {
      json <- maybeJson.toSuccess(noJson)
      collectionId <- (json \ "collectionId").asOpt[String].toSuccess(propertyNotFoundInJson("collectionId"))
      canWrite <- auth.canCreateItemInCollection(collectionId).leftMap(generalError(UNAUTHORIZED, _))
      hasAccess <- if (canWrite) {
        Success(true)
      } else {
        Failure(generalError(UNAUTHORIZED, "?"))
      }
      id <- createItem(collectionId).toSuccess(generalError(INTERNAL_SERVER_ERROR, "Error creating item"))
    } yield id

    accessResult.leftMap(e => HttpStatusMessage(e.code, e.message)).rightMap(_.toString).toEither

  }
}
