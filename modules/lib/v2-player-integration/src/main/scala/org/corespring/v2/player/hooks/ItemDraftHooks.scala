package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.Hooks.{ R, StatusMessage }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.client.{ hooks => containerHooks }
import org.corespring.conversion.qti.transformers.{ PlayerJsonToItem, ItemTransformer }
import org.corespring.drafts.errors.DraftError
import org.corespring.drafts.item.models._
import org.corespring.drafts.item.{ ItemDrafts => DraftsBackend, MakeDraftId }
import org.corespring.models.item.{ Item => ModelItem, PlayerDefinition }
import org.corespring.models.json.JsonFormatting
import org.corespring.services.{ OrgCollectionService, OrganizationService }
import org.corespring.services.item.ItemService
import org.corespring.v2.api.drafts.item.json.CommitJson
import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import play.api.Logger
import play.api.libs.json.{ JsValue, Json, _ }
import play.api.mvc.RequestHeader

import scala.concurrent.Future
import scala.language.implicitConversions
import scalaz.Scalaz._
import scalaz.{ Success, Validation }

trait DraftHelper {
  implicit def validationToEither[A](v: Validation[V2Error, A]): Either[StatusMessage, A] = v.leftMap { e => e.statusCode -> e.message }.toEither
  implicit def validationToOption[A](v: Validation[V2Error, A]): Option[StatusMessage] = v.swap.map { e => e.statusCode -> e.message }.toOption
}

class ItemDraftHooks(
  backend: DraftsBackend,
  itemService: ItemService,
  orgCollectionService: OrgCollectionService,
  transformer: ItemTransformer,
  val jsonFormatting: JsonFormatting,
  getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts],
  override implicit val containerContext: ContainerExecutionContext)
  extends containerHooks.DraftHooks
  with BaseItemHooks
  with LoadOrgAndOptions
  with ContainerConverters
  with MakeDraftId {

  override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = getOrgAndOptsFn.apply(request)

  private lazy val logger = Logger(classOf[ItemHooks])

  //TODO: Why do we load the draft twice? Once in DraftEditorHooks and once here
  override def load(draftId: String)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
    for {
      draftAndIdentity <- loadDraftAndIdentity(draftId, backend.loadOrCreate(_)(_, ignoreConflict = true))
      draft <- Success(draftAndIdentity._1)
      json <- Success(transformer.transformToV2Json(draft.change.data))
    } yield {
      logger.trace(s"draftId=$draftId, json=${Json.stringify(json)}")
      json
    }
  }

  private implicit class MkV2Error[A](v: Validation[DraftError, A]) {
    def v2Error: Validation[V2Error, A] = v.leftMap(e => generalError(e.msg))
  }

  private def loadDraftAndIdentity(id: String, loadFn: (OrgAndUser, DraftId) => Validation[DraftError, ItemDraft])(implicit rh: RequestHeader): Validation[V2Error, (ItemDraft, OrgAndUser)] = for {
    identity <- getOrgAndUser(rh)
    draftId <- mkDraftId(identity, id).v2Error
    draft <- loadFn(identity, draftId).v2Error
  } yield {
    (draft, identity)
  }

  override protected def update(draftId: String, json: JsValue, updateFn: (ModelItem, JsValue) => ModelItem)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
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

  def save(draftId: String, json: JsValue)(implicit h: RequestHeader): R[JsValue] = {
    update(draftId, json.as[JsObject], PlayerJsonToItem.wholeItem)
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
      orgCollectionService.getDefaultCollection(u.org.id).toOption.map { c =>
        ModelItem(
          collectionId = c.toString,
          playerDefinition = Some(PlayerDefinition("")))
      }
    }

    def randomDraftName = scala.util.Random.alphanumeric.take(12).mkString

    val result: Validation[V2Error, (String, String)] = for {
      identity <- getOrgAndUser(h)
      item <- mkItem(identity).toSuccess(generalError("Can't make a new item"))
      vid <- itemService.save(item, false).leftMap(e => generalError(e.message))
      draft <- backend.create(DraftId(vid.id, randomDraftName, identity.org.id), identity).v2Error
    } yield (vid.toString, draft.id.toString)

    result.leftMap { e => (e.statusCode -> e.message) }.toEither
  }

}
