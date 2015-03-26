package org.corespring.v2.player.hooks

import org.bson.types.ObjectId
import org.corespring.container.client.hooks.Hooks.R
import org.corespring.container.client.hooks.{ ItemDraftHooks => ContainerItemDraftHooks }
import org.corespring.drafts.item.models.{ SimpleUser, SimpleOrg, OrgAndUser, ItemDraft }
import org.corespring.drafts.item.services.ItemDraftService
import org.corespring.platform.core.models.User
import org.corespring.platform.core.models.item.{ Item => ModelItem, PlayerDefinition }
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.PlayerJsonToItem
import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.libs.json.{ JsValue, Json, _ }
import play.api.mvc.RequestHeader

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{ Success, Validation }

trait ItemDraftHooks extends ContainerItemDraftHooks with LoadOrgAndOptions {

  def draftService: ItemDraftService

  def itemService: ItemService

  def transform: ModelItem => JsValue

  private lazy val logger = V2LoggerFactory.getLogger("ItemHooks")

  override def load(itemId: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
    val result = for {
      draftId <- Some(new ObjectId(itemId))
      draft <- draftService.load(draftId)
    } yield {
      val item = draft.src.data
      transform(item)
    }

    result match {
      case None => Left((404, "Not Found"))
      case Some(json) => Right(json)
    }
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

  def authenticateUser(rh: RequestHeader): Option[User]

  private def loadDraftAndIdentity(draftId: String)(implicit rh: RequestHeader): Validation[V2Error, (ItemDraft, User)] = for {
    identity <- authenticateUser(rh).toSuccess(generalError("can't identify a user"))
    draft <- draftService.load(new ObjectId(draftId)).toSuccess(generalError(s"can't find draft with id: $draftId"))
  } yield {
    (draft, identity)
  }

  private def update(draftId: String, json: JsValue, updateFn: (ModelItem, JsValue) => ModelItem)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
    logger.debug(s"update draftId=$draftId")

    val out: Validation[V2Error, JsValue] = for {
      itemAndIdentity <- loadDraftAndIdentity(draftId)
      draft <- Success(itemAndIdentity._1)
      item <- Success(draft.src.data)
      _ <- Success(itemAndIdentity._2)
      updatedItem <- Success(updateFn(item, json))
    } yield {
      val update = draft.update(updatedItem)
      draftService.save(update)
      json
    }

    out.leftMap(e => e.statusCode -> e.message).toEither
  }

  private def baseDefinition(playerDef: Option[PlayerDefinition]): JsObject = Json.toJson(playerDef.getOrElse(new PlayerDefinition(Seq.empty, "", Json.obj(), "", None))).as[JsObject]

  private def savePartOfPlayerDef(draftId: String, json: JsObject)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
    val out: Validation[V2Error, JsValue] = for {
      draftAndIdentity <- loadDraftAndIdentity(draftId)
      draft <- Success(draftAndIdentity._1)
      item <- Success(draft.src.data)
      _ <- Success(draftAndIdentity._2)
      updatedItem <- Success(PlayerJsonToItem.playerDef(item, baseDefinition(item.playerDefinition) ++ json))
    } yield {
      val updated = draft.update(updatedItem)
      draftService.save(updated)
      json
    }

    out.leftMap(e => e.statusCode -> e.message).toEither
  }

  private def getOrgAndUser(h: RequestHeader): Validation[V2Error, OrgAndUser] = {
    getOrgAndOptions(h).map { o =>
      OrgAndUser(SimpleOrg(o.org), o.user.map(SimpleUser.fromUser))
    }
  }

  override def create(itemId: String)(implicit h: RequestHeader): R[String] = {
    for {
      identity <- getOrgAndUser(h)
      vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
      item <- itemService.findOneById(vid).toSuccess(cantFindItemWithId(vid))
      draft <- Success(ItemDraft(item, identity))
    } yield draft.id.toString

  }

  override def delete(draftId: String)(implicit h: RequestHeader): R[JsValue] = ???

  override def commit(draftId: String, force: Boolean)(implicit h: RequestHeader): R[JsValue] = ???

}
