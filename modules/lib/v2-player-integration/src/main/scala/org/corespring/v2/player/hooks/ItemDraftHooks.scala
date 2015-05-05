package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.Hooks.{ R, StatusMessage }
import org.corespring.container.client.hooks.{ ItemDraftHooks => ContainerItemDraftHooks }
import org.corespring.drafts.errors.DraftError
import org.corespring.drafts.item.models._
import org.corespring.drafts.item.{ ItemDrafts => DraftsBackend, MakeDraftId }
import org.corespring.platform.core.models.item.{ Item => ModelItem, PlayerDefinition }
import org.corespring.platform.core.services.item.ItemService
import org.corespring.qtiToV2.transformers.PlayerJsonToItem
import org.corespring.v2.api.drafts.item.json.CommitJson
import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.auth.services.OrgService
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.libs.json.{ JsValue, Json, _ }
import play.api.mvc.RequestHeader

import scala.language.implicitConversions
import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

trait DraftHelper {
  implicit def validationToEither[A](v: Validation[V2Error, A]): Either[StatusMessage, A] = v.leftMap { e => e.statusCode -> e.message }.toEither
  implicit def validationToOption[A](v: Validation[V2Error, A]): Option[StatusMessage] = v.swap.map { e => e.statusCode -> e.message }.toOption
}

trait ItemDraftHooks
  extends ContainerItemDraftHooks
  with LoadOrgAndOptions
  with DraftHelper
  with MakeDraftId {

  def backend: DraftsBackend
  def itemService: ItemService
  def orgService: OrgService
  def transform: ModelItem => JsValue

  private lazy val logger = V2LoggerFactory.getLogger("ItemHooks")

  //TODO: Why do we load the draft twice? Once in DraftEditorHooks and once here
  override def load(draftId: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
    for {
      draftAndIdentity <- loadDraftAndIdentity(draftId, backend.loadOrCreate(_)(_, ignoreConflict = true))
      draft <- Success(draftAndIdentity._1)
      json <- Success(transform(draft.change.data))
    } yield {
      logger.trace(s"draftId=$draftId, json=${Json.stringify(json)}")
      Json.obj("item" -> json)
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
  override def saveCustomScoring(draftId: String, customScoring: String)(implicit header: RequestHeader): R[JsValue] = {

    def updateCustomScoring(item: ModelItem, json: JsValue): ModelItem = {
      val updatedDefinition = item.playerDefinition.map { pd =>
        new PlayerDefinition(pd.files, pd.xhtml, pd.components, pd.summaryFeedback, Some(customScoring))
      }.getOrElse {
        PlayerDefinition(Seq.empty, "", Json.obj(), "", Some(customScoring))
      }
      item.copy(playerDefinition = Some(updatedDefinition))
    }

    update(draftId, Json.obj("customScoring" -> customScoring), updateCustomScoring)
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

  private implicit class MkV2Error[A](v: Validation[DraftError, A]) {
    def v2Error: Validation[V2Error, A] = {
      v.leftMap { e => generalError(e.msg) }
    }
  }

  private def loadDraftAndIdentity(id: String, loadFn: (OrgAndUser, DraftId) => Validation[DraftError, ItemDraft])(implicit rh: RequestHeader): Validation[V2Error, (ItemDraft, OrgAndUser)] = for {
    identity <- getOrgAndUser(rh)
    draftId <- mkDraftId(identity, id).v2Error
    draft <- loadFn(identity, draftId).v2Error
  } yield {
    (draft, identity)
  }

  protected def update(draftId: String, json: JsValue, updateFn: (ModelItem, JsValue) => ModelItem)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
    logger.debug(s"update draftId=$draftId")
    for {
      draftAndIdentity <- loadDraftAndIdentity(draftId, backend.load(_)(_))
      draft <- Success(draftAndIdentity._1)
      item <- Success(draft.change.data)
      updatedItem <- Success(updateFn(item, json))
      update <- Success(draft.mkChange(updatedItem))
      saved <- backend.save(draftAndIdentity._2)(update).v2Error
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

  override def delete(id: String)(implicit h: RequestHeader): R[JsValue] = Future {
    for {
      identity <- getOrgAndUser(h)
      draftId <- mkDraftId(identity, id).leftMap { e => generalError(e.msg) }
      draft <- backend.remove(identity)(draftId).leftMap { e => generalError(e.msg) }
    } yield Json.obj("id" -> id)
  }

  override def commit(id: String, force: Boolean)(implicit h: RequestHeader): R[JsValue] = Future {
    for {
      identity <- getOrgAndUser(h)
      draftId <- mkDraftId(identity, id).leftMap { e => generalError(e.msg) }
      draft <- backend.load(identity)(draftId).v2Error
      result <- backend.commit(identity)(draft, force).v2Error
    } yield CommitJson(result)
  }

  override def createItemAndDraft()(implicit h: RequestHeader): R[(String, String)] = Future {
    def mkItem(u: OrgAndUser) = {
      orgService.defaultCollection(u.org.id).map { c =>
        ModelItem(
          collectionId = Some(c.toString),
          playerDefinition = Some(PlayerDefinition("")))
      }
    }

    def randomDraftName = scala.util.Random.alphanumeric.take(12).mkString

    val result: Validation[V2Error, (String, String)] = for {
      identity <- getOrgAndUser(h)
      item <- mkItem(identity).toSuccess(generalError("Can't make a new item"))
      vid <- itemService.save(item, false) match {
        case Left(m) => Failure(generalError(m))
        case Right(vid) => Success(vid)
      }
      draft <- backend.create(DraftId(vid.id, randomDraftName, identity.org.id), identity).v2Error
    } yield (vid.toString, draft.id.toString)

    result.leftMap { e => (e.statusCode -> e.message) }.toEither
  }

}
