package org.corespring.v2.player.hooks

import org.bson.types.ObjectId
import org.corespring.container.client.hooks.Hooks.StatusMessage
import org.corespring.container.client.hooks.{ ItemDraftHooks => ContainerItemDraftHooks }
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

trait ItemDraftHooks extends ContainerItemDraftHooks with LoadOrgAndOptions {

  def transform: ModelItem => JsValue

  def auth: ItemAuth[OrgAndOpts]

  lazy val logger = V2LoggerFactory.getLogger("ItemHooks")

  override def load(itemId: String)(implicit header: RequestHeader): Future[Either[StatusMessage, JsValue]] = Future {
    val item: Validation[V2Error, JsValue] = for {
      identity <- getOrgAndOptions(header)
      vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
      item <- auth.loadForRead(itemId)(identity)
    } yield transform(item)

    item.leftMap(e => e.statusCode -> e.message).toEither
  }

  override def saveProfile(itemId: String, json: JsValue)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = {
    logger.debug(s"saveProfile itemId=$itemId")
    def withKey(j: JsValue) = Json.obj("profile" -> j)
    update(itemId, json, PlayerJsonToItem.profile).map { e => e.rightMap(withKey) }
  }

  override def saveSupportingMaterials(itemId: String, json: JsValue)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = {
    logger.debug(s"saveSupportingMaterials itemId=$itemId")
    update(itemId, Json.obj("supportingMaterials" -> json), PlayerJsonToItem.supportingMaterials)
  }

  override def saveComponents(itemId: String, json: JsValue)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = {
    savePartOfPlayerDef(itemId, Json.obj("components" -> json))
  }

  override def saveXhtml(itemId: String, xhtml: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = {
    savePartOfPlayerDef(itemId, Json.obj("xhtml" -> xhtml))
  }

  override def saveSummaryFeedback(itemId: String, feedback: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = {
    savePartOfPlayerDef(itemId, Json.obj("summaryFeedback" -> feedback))
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
      identity <- getOrgAndOptions(header)
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

  private def loadItemAndIdentity(itemId: String)(implicit rh: RequestHeader): Validation[V2Error, (ModelItem, OrgAndOpts)] = for {
    identity <- getOrgAndOptions(rh)
    vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
    item <- auth.loadForWrite(itemId)(identity)
    collectionId <- item.collectionId.toSuccess(noCollectionIdForItem(vid))
  } yield {
    (item, identity)
  }

  private def update(itemId: String, json: JsValue, updateFn: (ModelItem, JsValue) => ModelItem)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
    logger.debug(s"saveProfile itemId=$itemId")

    val out: Validation[V2Error, JsValue] = for {
      itemAndIdentity <- loadItemAndIdentity(itemId)
      item <- Success(itemAndIdentity._1)
      identity <- Success(itemAndIdentity._2)
      updatedItem <- Success(updateFn(item, json))
    } yield {
      auth.save(updatedItem, createNewVersion = false)(identity)
      json
    }

    out.leftMap(e => e.statusCode -> e.message).toEither
  }

  private def baseDefinition(playerDef: Option[PlayerDefinition]): JsObject = Json.toJson(playerDef.getOrElse(new PlayerDefinition(Seq.empty, "", Json.obj(), "", None))).as[JsObject]

  private def savePartOfPlayerDef(itemId: String, json: JsObject)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
    val out: Validation[V2Error, JsValue] = for {
      itemAndIdentity <- loadItemAndIdentity(itemId)
      item <- Success(itemAndIdentity._1)
      identity <- Success(itemAndIdentity._2)
      updatedItem <- Success(PlayerJsonToItem.playerDef(item, baseDefinition(item.playerDefinition) ++ json))
    } yield {
      auth.save(updatedItem, false)(identity)
      json
    }

    out.leftMap(e => e.statusCode -> e.message).toEither
  }

}
