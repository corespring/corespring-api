package org.corespring.v2player.integration.controllers.editor

import org.bson.types.ObjectId
import org.corespring.container.client.actions.{ ItemHooks => ContainerItemHooks }
import org.corespring.platform.core.models
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item.resource.{ BaseFile, Resource }
import org.corespring.platform.core.models.item.{ PlayerDefinition, Item => ModelItem }
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.actionBuilders.LoadOrgAndOptions
import org.corespring.v2player.integration.controllers.editor.json.PlayerJsonToItem
import org.corespring.v2player.integration.errors.Errors._
import org.corespring.v2player.integration.errors.V2Error
import org.slf4j.LoggerFactory
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

trait ItemHooks
  extends ContainerItemHooks
  with LoadOrgAndOptions {

  override protected lazy val logger = LoggerFactory.getLogger("v2Player.itemHooks")

  def itemService: ItemService

  def orgService: OrganizationService

  def transform: ModelItem => JsValue

  implicit def executionContext: ExecutionContext

  override def load(itemId: String)(implicit header: RequestHeader): Future[Either[SimpleResult, JsValue]] = Future {

    val item: Validation[V2Error, ModelItem] = for {
      id <- VersionedId(itemId).toSuccess(cantParseItemId)
      item <- itemService.findOneById(id).toSuccess(cantFindItemWithId(id))
      orgIdAndOptions <- getOrgIdAndOptions(header).leftMap(s => noOrgIdAndOptions(header))
      collectionId <- item.collectionId.toSuccess(noCollectionIdForItem(id))
      hasAccess <- if (orgService.canAccessCollection(orgIdAndOptions._1, new ObjectId(collectionId), Permission.Write)) {
        Success(item)
      } else {
        Failure(orgCantAccessCollection(orgIdAndOptions._1, collectionId))
      }
    } yield item

    item match {
      case Success(i) => {
        val containerJson = transform(i)
        Right(containerJson)
      }
      case Failure(err) => Left(Status(err.code)(err.message))
    }
  }

  private def supportingMaterials(item: ModelItem, json: JsValue): ModelItem = {
    implicit val baseFileFormat = BaseFile.BaseFileFormat
    (json \ "supportingMaterials") match {
      case undefined: JsUndefined => item
      case _ => (json \ "supportingMaterials") match {
        case array: JsArray => item.copy(
          supportingMaterials =
            array.as[List[JsObject]].map(supportingMaterial => Resource(
              id = (supportingMaterial \ "id").asOpt[String].map(new ObjectId(_)),
              name = (supportingMaterial \ "name").as[String],
              materialType = (supportingMaterial \ "materialType").asOpt[String],
              files = (supportingMaterial \ "files").asOpt[List[JsObject]].getOrElse(List.empty[JsObject])
                .map(f => Json.fromJson[BaseFile](f).get))).map(m => (m.id match {
              case Some(id) => m
              case None => m.copy(id = Some(new ObjectId()))
            })))
        case _ => throw new IllegalArgumentException("supportingMaterials must be an array")
      }
    }
  }

  override def save(itemId: String, json: JsValue)(implicit header: RequestHeader): Future[Either[SimpleResult, JsValue]] = Future {

    logger.debug(s"save - itemId: $itemId")
    logger.trace(s"save - json: ${Json.stringify(json)}")

    /** an implementation for the container to save its definition */
    def convertAndSave(itemId: String, item: ModelItem): Option[JsValue] = {

      val updates = Seq(
        (item: ModelItem, json: JsValue) => supportingMaterials(item, json),
        (item: ModelItem, json: JsValue) => (json \ "profile").asOpt[JsObject].map { obj => PlayerJsonToItem.profile(item, obj) }.getOrElse(item),
        (item: ModelItem, json: JsValue) => PlayerJsonToItem.playerDef(item, json))

      val updatedItem: ModelItem = updates.foldRight(item) { (fn, i) => fn(i, json) }

      itemService.save(updatedItem, false)
      Some(transform(updatedItem))
    }

    val out: Validation[V2Error, JsValue] = for {
      vid <- VersionedId(itemId).toSuccess(cantParseItemId)
      item <- itemService.findOneById(vid).toSuccess(cantFindItemWithId(vid))
      orgIdAndOptions <- getOrgIdAndOptions(header).leftMap(s => noOrgIdAndOptions(header))
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

    val definition = PlayerDefinition(Seq(), "<div>I'm a new item</div>", Json.obj(), "")
    val item = models.item.Item(
      collectionId = Some(collectionId),
      playerDefinition = Some(definition))
    itemService.insert(item)
  }

  override def create(maybeJson: Option[JsValue])(implicit header: RequestHeader): Future[Either[(Int, String), String]] = Future {

    val accessResult: Validation[V2Error, String] = for {
      json <- maybeJson.toSuccess(noJson)
      collectionId <- (json \ "collectionId").asOpt[String].toSuccess(propertyNotFoundInJson("collectionId"))
      orgIdAndOptions <- getOrgIdAndOptions(header).leftMap(s => noOrgIdAndOptions(header))
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
