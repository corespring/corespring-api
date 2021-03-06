package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.Hooks.{R, StatusMessage}
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.client.{hooks => containerHooks}
import org.corespring.conversion.qti.transformers.{ItemTransformer, PlayerJsonToItem}
import org.corespring.drafts.errors.DraftError
import org.corespring.drafts.item.models._
import org.corespring.drafts.item.{MakeDraftId, ItemDrafts => DraftsBackend}
import org.corespring.models.DisplayConfig
import org.corespring.models.item.{PlayerDefinition, Item => ModelItem}
import org.corespring.models.json.JsonFormatting
import org.corespring.services.{OrgCollectionService, OrganizationService}
import org.corespring.services.item.ItemService
import org.corespring.v2.api.drafts.item.json.CommitJson
import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.auth.models.{DisplayConfigJson, OrgAndOpts}
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import play.api.Logger
import play.api.libs.json.{JsValue, Json, _}
import play.api.mvc.RequestHeader

import scala.concurrent.Future
import scala.language.implicitConversions
import scalaz.Scalaz._
import scalaz.{Success, Validation}

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
  val playerJsonToItem: PlayerJsonToItem,
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
  override def load(draftId: String)(implicit header: RequestHeader): Future[Either[(Int, String), (JsValue, JsValue)]] = Future {
    for {
      identity <- getOrgAndOptions(header)
      draft <- loadDraft(draftId, identity, backend.loadOrCreate(_)(_, ignoreConflict = true))
      json <- Success(transformer.transformToV2Json(draft.change.data))
    } yield {
      logger.trace(s"draftId=$draftId, json=${Json.stringify(json)}")
      (json, DisplayConfigJson(identity))
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

  private def loadDraft(id: String, identity: OrgAndOpts, loadFn: (OrgAndUser, DraftId) => Validation[DraftError, ItemDraft])(implicit rh: RequestHeader): Validation[V2Error, ItemDraft] = {
    val simpleIdentity = OrgAndUser(SimpleOrg.fromOrganization(identity.org), identity.user.map(SimpleUser.fromUser))
    for {
      identity <- getOrgAndUser(rh)
      draftId <- mkDraftId(simpleIdentity, id).v2Error
      draft <- loadFn(simpleIdentity, draftId).v2Error
    } yield draft
  }

  override protected def update(draftId: String, json: JsValue, updateFn: (ModelItem, JsValue) => ModelItem)(implicit header: RequestHeader): Future[Either[(Int, String), JsValue]] = Future {
    logger.debug(s"update draftId=$draftId json=$json")
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
    update(draftId, json.as[JsObject], playerJsonToItem.wholeItem)
  }

  override def commit(id: String, force: Boolean)(implicit h: RequestHeader): R[JsValue] = Future {
    for {
      identity <- getOrgAndUser(h)
      draftId <- mkDraftId(identity, id).leftMap { e => generalError(e.msg) }
      draft <- backend.load(identity)(draftId).v2Error
      result <- backend.commit(identity)(draft, force).v2Error
    } yield CommitJson(result)
  }

  override def createSingleComponentItemDraft(collectionId: Option[String], componentType: String, key: String, defaultData: JsObject)(implicit r: RequestHeader): R[(String, String)] = {
    val xhtml = s"""<div><div $componentType="" id="$key"></div></div>"""
    createItemAndDraft(r) { (u: OrgAndUser) =>
      mkItem(collectionId, u, PlayerDefinition(xhtml = xhtml, components = Json.obj(key -> defaultData)))
    }
  }

  override def createItemAndDraft(collectionId: Option[String])(implicit h: RequestHeader): R[(String, String)] = {
    createItemAndDraft(h) { (u: OrgAndUser) =>
      mkItem(collectionId, u, PlayerDefinition(""))
    }
  }

  private def mkItem(collectionId: Option[String], u: OrgAndUser, playerDefinition: PlayerDefinition) = {

    logger.debug(s"function=mkItem, collectionId=$collectionId")

    lazy val default = orgCollectionService.getDefaultCollection(u.org.id)

    collectionId.orElse(default.toOption.map(_.id.toString)).map { c =>
      logger.debug(s"function=mkItem, c=$c")
      ModelItem(
        collectionId = c,
        playerDefinition = Some(playerDefinition))
    }
  }

  private def createItemAndDraft(h: RequestHeader)(mkItem: OrgAndUser => Option[ModelItem]): R[(String, String)] = Future {

    def randomDraftName = scala.util.Random.alphanumeric.take(12).mkString

    val result: Validation[V2Error, (String, String)] = for {
      identity <- getOrgAndUser(h)
      item <- mkItem(identity).toSuccess(generalError("Can't make a new item"))
      vid <- itemService.save(item, false).leftMap(e => generalError(e.message))
      draft <- backend.create(DraftId(vid.id, randomDraftName, identity.org.id), identity).v2Error
    } yield (vid.id.toString, draft.id.name)
    result.leftMap { e => (e.statusCode -> e.message) }.toEither
  }
}
