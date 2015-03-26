package org.corespring.v2.player.hooks

import org.bson.types.ObjectId
import org.corespring.container.client.hooks.Hooks.{ R, StatusMessage }
import org.corespring.container.client.hooks.{ ItemDraftHooks => ContainerItemDraftHooks }
import org.corespring.drafts.item.models.{ ItemDraft, OrgAndUser, SimpleOrg, SimpleUser }
import org.corespring.drafts.item.{ ItemDrafts => DraftsBackend }
import org.corespring.platform.core.models.item.{ Item => ModelItem, PlayerDefinition }
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.PlayerJsonToItem
import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.libs.json.{ JsValue, Json, _ }
import play.api.mvc.RequestHeader

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{ Success, Validation }

trait ItemDraftHooks extends ContainerItemDraftHooks with LoadOrgAndOptions {

  def backend: DraftsBackend
  def itemService: ItemService

  def transform: ModelItem => JsValue

  private lazy val logger = V2LoggerFactory.getLogger("ItemHooks")

  override def load(itemId: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
    val result = for {
      draftId <- Some(new ObjectId(itemId))
      draft <- backend.load(draftId)
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

  private def loadDraftAndIdentity(draftId: String)(implicit rh: RequestHeader): Validation[V2Error, (ItemDraft, OrgAndUser)] = for {
    identity <- getOrgAndUser(rh)
    draft <- backend.load(new ObjectId(draftId)).toSuccess(generalError(s"can't find draft with id: $draftId"))
  } yield {
    (draft, identity)
  }

  private def update(draftId: String, json: JsValue, updateFn: (ModelItem, JsValue) => ModelItem)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
    logger.debug(s"update draftId=$draftId")
    for {
      draftAndIdentity <- loadDraftAndIdentity(draftId)
      draft <- Success(draftAndIdentity._1)
      item <- Success(draft.src.data)
      updatedItem <- Success(updateFn(item, json))
      update <- Success(draft.update(updatedItem))
      saved <- backend.save(update).leftMap { de => generalError(de.msg) }
    } yield json
  }

  private def baseDefinition(playerDef: Option[PlayerDefinition]): JsObject = Json.toJson(playerDef.getOrElse(new PlayerDefinition(Seq.empty, "", Json.obj(), "", None))).as[JsObject]

  private def savePartOfPlayerDef(draftId: String, json: JsObject)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = {
    update(draftId, json, (i, _) => PlayerJsonToItem.playerDef(i, baseDefinition(i.playerDefinition) ++ json))(header)
  }

  private def getOrgAndUser(h: RequestHeader): Validation[V2Error, OrgAndUser] = {
    getOrgAndOptions(h).map { o =>
      OrgAndUser(SimpleOrg.fromOrganization(o.org), o.user.map(SimpleUser.fromUser))
    }
  }

  import scala.language.implicitConversions

  implicit def vToR[A](v: Validation[V2Error, A]): Either[StatusMessage, A] = v.leftMap { e => e.statusCode -> e.message }.toEither

  override def create(itemId: String)(implicit h: RequestHeader): R[String] = Future {
    for {
      identity <- getOrgAndUser(h)
      vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
      draft <- backend.create(vid.id, identity).toSuccess(generalError("Error creating draft"))
    } yield draft.id.toString
  }

  override def delete(draftId: String)(implicit h: RequestHeader): R[JsValue] = ???

  override def commit(draftId: String, force: Boolean)(implicit h: RequestHeader): R[JsValue] = ???

}
