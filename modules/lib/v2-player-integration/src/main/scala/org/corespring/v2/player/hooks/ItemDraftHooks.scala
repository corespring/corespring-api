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
import org.corespring.v2.api.drafts.item.json.CommitJson
import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.libs.json.{ JsValue, Json, _ }
import play.api.mvc.RequestHeader

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

trait DraftHelper {

  import scala.language.implicitConversions

  def getOid(s: String, context: String): Validation[V2Error, ObjectId] = try { Success(new ObjectId(s)) } catch { case t: Throwable => Failure(invalidObjectId(s, context)) }
  implicit def validationToEither[A](v: Validation[V2Error, A]): Either[StatusMessage, A] = v.leftMap { e => e.statusCode -> e.message }.toEither
  implicit def validationToOption[A](v: Validation[V2Error, A]): Option[StatusMessage] = v.swap.map { e => e.statusCode -> e.message }.toOption
}

trait ItemDraftHooks extends ContainerItemDraftHooks with LoadOrgAndOptions with DraftHelper {

  def backend: DraftsBackend
  def itemService: ItemService

  def transform: ModelItem => JsValue

  private lazy val logger = V2LoggerFactory.getLogger("ItemHooks")

  override def load(draftId: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
    for {
      draftAndIdentity <- loadDraftAndIdentity(draftId)
      draft <- Success(draftAndIdentity._1)
      json <- Success(transform(draft.src.data))
    } yield json
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
    draft <- backend.load(identity)(new ObjectId(draftId)).toSuccess(generalError(s"can't find draft with id: $draftId"))
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
      saved <- backend.save(draftAndIdentity._2)(update).leftMap { de => generalError(de.msg) }
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

  override def create(itemId: String)(implicit h: RequestHeader): R[String] = Future {
    for {
      identity <- getOrgAndUser(h)
      vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
      draft <- backend.create(vid.id, identity).toSuccess(generalError("Error creating draft"))
    } yield draft.id.toString
  }

  override def delete(draftId: String)(implicit h: RequestHeader): R[JsValue] = Future {
    for {
      identity <- getOrgAndUser(h)
      oid <- getOid(draftId, "delete -> draftId")
      draft <- backend.removeDraftByIdAndUser(oid, identity).leftMap { e => generalError(e.msg) }
    } yield Json.obj("id" -> draftId)
  }

  override def commit(draftId: String, force: Boolean)(implicit h: RequestHeader): R[JsValue] = Future {
    for {
      identity <- getOrgAndUser(h)
      oid <- getOid(draftId, "commit -> draftId")
      draft <- backend.load(identity)(oid).toSuccess(generalError(s"Can't find draft with id $draftId"))
      result <- backend.commit(identity)(draft, force).leftMap { e => generalError(e.msg) }
    } yield CommitJson(result)
  }

}
