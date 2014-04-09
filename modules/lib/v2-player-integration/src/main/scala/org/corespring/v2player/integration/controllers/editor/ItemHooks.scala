package org.corespring.v2player.integration.controllers.editor

import org.bson.types.ObjectId
import org.corespring.container.client.actions.{ ItemHooks => ContainerItemHooks }
import org.corespring.platform.core.models
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item.{ Item => ModelItem, PlayerDefinition }
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.actionBuilders.LoadOrgAndOptions
import org.corespring.v2player.integration.controllers.editor.json.PlayerJsonToItem
import org.corespring.v2player.integration.errors.Errors._
import org.corespring.v2player.integration.errors.Errors.cantFindItemWithId
import org.corespring.v2player.integration.errors.Errors.noCollectionIdForItem
import org.corespring.v2player.integration.errors.Errors.noOrgIdAndOptions
import org.corespring.v2player.integration.errors.Errors.orgCantAccessCollection
import org.corespring.v2player.integration.errors.Errors.propertyNotFoundInJson
import org.corespring.v2player.integration.errors.V2Error
import play.api.http.Status._
import play.api.libs.json.{ JsObject, Json, JsValue }
import play.api.mvc.Results._
import play.api.mvc._
import scala.Some
import scala.concurrent.{ ExecutionContext, Future }
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }
import org.slf4j.LoggerFactory

trait ItemHooks
  extends ContainerItemHooks
  with LoadOrgAndOptions {

  override protected lazy val logger = LoggerFactory.getLogger("v2Player.itemHooks")

  def itemService: ItemService

  def orgService: OrganizationService

  def transform: ModelItem => JsValue

  implicit def executionContext: ExecutionContext

  override def load(itemId: String)(implicit header: RequestHeader): Future[Either[SimpleResult, JsValue]] = Future {
    val item = for {
      id <- VersionedId(itemId)
      item <- itemService.findOneById(id)
    } yield item

    item.map {
      i =>
        val containerJson = transform(i)
        Right(containerJson)
    }.getOrElse(Left(NotFound("?")))
  }

  override def save(itemId: String, json: JsValue)(implicit header: RequestHeader): Future[Either[SimpleResult, JsValue]] = Future {

    logger.debug(s"save - itemId: $itemId")
    logger.trace(s"save - json: ${Json.stringify(json)}")

    /** an implementation for the container to save its definition */
    def convertAndSave(itemId: String, item: ModelItem): Option[JsValue] = {

      val updates = Seq(
        (item: ModelItem, json: JsValue) => (json \ "profile").asOpt[JsObject].map { obj => PlayerJsonToItem.profile(item, obj) }.getOrElse(item),
        (item: ModelItem, json: JsValue) => PlayerJsonToItem.playerDef(item, json))

      val updatedItem: ModelItem = updates.foldRight(item) { (fn, i) => fn(i, json) }

      itemService.save(updatedItem, false)
      Some(json)
    }

    val out: Validation[V2Error, JsValue] = for {
      vid <- VersionedId(itemId).toSuccess(cantParseItemId)
      item <- itemService.findOneById(vid).toSuccess(cantFindItemWithId(vid))
      orgIdAndOptions <- getOrgIdAndOptions(header).toSuccess(noOrgIdAndOptions(header))
      collectionId <- item.collectionId.toSuccess(noCollectionIdForItem(vid))
      hasAccess <- if (orgService.canAccessCollection(orgIdAndOptions._1, new ObjectId(collectionId), Permission.Write)) {
        Success(true)
      } else {
        Failure(orgCantAccessCollection(orgIdAndOptions._1, collectionId))
      }
      result <- convertAndSave(itemId, item).toSuccess(errorSaving)
    } yield {
      result
    }

    out match {
      case Success(json) => Right(json)
      case Failure(err) => Left(Status(err.code)(err.message))
    }
  }

  private def createItem(collectionId: String): Option[VersionedId[ObjectId]] = {

    val definition = PlayerDefinition(Seq(), "<div>I'm a new item</div>", Json.obj())
    val item = models.item.Item(
      collectionId = Some(collectionId),
      playerDefinition = Some(definition))
    itemService.insert(item)
  }

  override def create(maybeJson: Option[JsValue])(implicit header: RequestHeader): Future[Either[(Int, String), String]] = Future {

    val accessResult: Validation[V2Error, String] = for {
      json <- maybeJson.toSuccess(noJson)
      collectionId <- (json \ "collectionId").asOpt[String].toSuccess(propertyNotFoundInJson("collectionId"))
      orgIdAndOptions <- getOrgIdAndOptions(header).toSuccess(noOrgIdAndOptions(header))
      access <- if (orgService.canAccessCollection(orgIdAndOptions._1, new ObjectId(collectionId), Permission.Write)) {
        Success(true)
      } else {
        Failure(orgCantAccessCollection(orgIdAndOptions._1, collectionId))
      }
    } yield collectionId

    accessResult match {
      case Success(collectionId) => {
        createItem(collectionId).map {
          id =>
            Right(id.toString)
        }.getOrElse(Left(BAD_REQUEST, "Access failed"))
      }
      case Failure(err) => Left(err.code, err.message)
    }
  }

}
