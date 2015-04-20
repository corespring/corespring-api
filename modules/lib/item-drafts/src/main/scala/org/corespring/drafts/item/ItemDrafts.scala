package org.corespring.drafts.item

import com.mongodb.{ CommandResult, WriteResult }
import org.bson.types.ObjectId
import org.corespring.drafts.errors._
import org.corespring.drafts.item.models._
import org.corespring.drafts.item.services.{ CommitService, ItemDraftService }
import org.corespring.drafts.{ Src, Drafts }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.{ ItemPublishingService, ItemService }
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.DateTime
import play.api.Logger

import scalaz.Scalaz._
import scalaz.{ ValidationNel, Failure, Success, Validation }

case class DraftCloneResult(itemId: VersionedId[ObjectId], draftId: DraftId)

case class ItemDraftIsOutOfDate(d : ItemDraft, src : Src[VersionedId[ObjectId], Item]) extends DraftIsOutOfDate[DraftId, VersionedId[ObjectId], Item](d, src)

trait ItemDrafts
  extends Drafts[
    DraftId,
    VersionedId[ObjectId],
    Item,
    OrgAndUser,
    ItemDraft,
    ItemCommit,
    ItemDraftIsOutOfDate] {

  protected val logger = Logger(classOf[ItemDrafts].getName)

  def itemService: ItemService with ItemPublishingService

  def draftService: ItemDraftService

  def commitService: CommitService

  def assets: ItemDraftAssets

  def collection = draftService.collection

  /** Check that the user may create the draft for the given src id */
  protected def userCanCreateDraft(id: VersionedId[ObjectId], user: OrgAndUser): Boolean

  def owns(user: OrgAndUser)(id: DraftId) = draftService.owns(user, id)

  def remove(requester: OrgAndUser)(id: DraftId) = {
    logger.debug(s"function=remove, id=$id")
    for {
      _ <- if (owns(requester)(id)) Success(true) else Failure(UserCantRemove(requester, id))
      _ <- draftService.remove(id) match {
        case true => Success()
        case false => Failure(DeleteDraftFailed(id))
      }
      _ <- assets.deleteDraft(id)
    } yield id
  }

  def listForOrg(orgId: ObjectId) = draftService.listForOrg(orgId)

  def listByItemAndOrgId(itemId: VersionedId[ObjectId], orgId: ObjectId) = draftService.listByItemAndOrgId(itemId, orgId).toSeq

  def publish(user: OrgAndUser)(draftId: DraftId): Validation[DraftError, VersionedId[ObjectId]] = for {
    d <- draftService.load(draftId).toSuccess(LoadDraftFailed(draftId.toString))
    commit <- commit(user)(d)
    publishResult <- if (itemService.publish(commit.srcId)) Success(true) else Failure(PublishItemError(d.parent.id))
    deleteResult <- removeNonConflictingDraftsForOrg(draftId.itemId, user.org.id)
  } yield {
    commit.srcId
  }

  def load(user: OrgAndUser)(draftId: DraftId): Validation[DraftError, ItemDraft] = {
    if (draftService.owns(user, draftId)) {
      draftService.load(draftId).toSuccess(LoadDraftFailed(draftId.toString))
    } else {
      Failure(UserCantLoad(user, draftId))
    }
  }

  def cloneDraft(requester: OrgAndUser)(draftId: DraftId): Validation[DraftError, DraftCloneResult] = for {
    d <- load(requester)(draftId)
    itemId <- Success(VersionedId(ObjectId.get))
    vid <- itemService.save(d.parent.data.copy(id = itemId, published = false)).disjunction.validation.leftMap { s => SaveDraftFailed(s) }
    newDraft <- create(vid, requester)
  } yield DraftCloneResult(vid, newDraft.id)

  protected def removeNonConflictingDraftsForOrg(itemId: ObjectId, orgId: ObjectId): Validation[DraftError, ObjectId] = {
    def v(v: Validation[DraftError, Unit]): ValidationNel[DraftError, Unit] = v.toValidationNel
    val out = for {
      ids <- Success(draftService.removeNonConflictingDraftsForOrg(itemId, orgId))
      deleteComplete <- assets.deleteDrafts(ids: _*).toList.traverseU(v)
    } yield ids
    out.bimap[DraftError, ObjectId](errs => RemoveDraftFailed(errs.list), _ => itemId)
  }

  /**
   * Creates a draft for the target data.
   */
  override def create(id: VersionedId[ObjectId], user: OrgAndUser, expires: Option[DateTime]): Validation[DraftError, ItemDraft] = {

    def mkDraft(srcId: VersionedId[ObjectId], src: Item, user: OrgAndUser): Validation[DraftError, ItemDraft] = {
      require(src.published == false, s"You can only create an ItemDraft from an unpublished item: ${src.id}")
      val draft = ItemDraft(src, user)
      assets.copyItemToDraft(src.id, draft.id).map { _ => draft }
    }

    for {
      canCreate <- if (userCanCreateDraft(id, user)) Success(true) else Failure(UserCantCreate(user, id))
      unpublishedItem <- itemService.getOrCreateUnpublishedVersion(id).toSuccess(GetUnpublishedItemError(id))
      draft <- mkDraft(id, unpublishedItem, user)
      saved <- save(user)(draft)
    } yield draft
  }

  //get the conflict
  def conflict(user: OrgAndUser)(draftId: DraftId): Validation[DraftError, Option[Conflict]] = {
    for {
      d <- draftService.load(draftId).toSuccess(LoadDraftFailed(draftId.toIdString))
      i <- itemService.getOrCreateUnpublishedVersion(d.parent.id).toSuccess(CantFindLatestSrc(d.parent.id))
    } yield {
      if (d.parent.data == i) {
        logger.debug(s"function=conflict, the parent matches the item - no conflicts found")
        None
      } else {
        Some(Conflict(d, i))
      }
    }
  }

  /** load a draft for the src <VID> for that user if not conflicted, if not found create it */
  override def loadOrCreate(requester: OrgAndUser)(id: DraftId, ignoreConflict: Boolean = false): Validation[DraftError, ItemDraft] = {
    val draft: Option[ItemDraft] = draftService.load(id)

    def failIfConflicted(d: ItemDraft): Validation[DraftError, ItemDraft] = {
      itemService.getOrCreateUnpublishedVersion(d.parent.id).map { i =>
        if (draftParentMatchesItem(d.parent.data, i) || ignoreConflict) {
          logger.debug(s"function=failIfConflicted, ignoreConflict=$ignoreConflict, (draft.parent == item)=${d.parent == i}")
          Success(d)
        } else {
          Failure(draftIsOutOfDate(d, ItemSrc(i)))
        }
      }.getOrElse(Failure(GeneralError("can't find unpublished version")))
    }

    draft.map(failIfConflicted).getOrElse(create(VersionedId(id.itemId), requester))
  }

  def draftParentMatchesItem(parent:Item, item:Item) = {
    parent.taskInfo == item.taskInfo &&
    parent.playerDefinition == item.playerDefinition &&
    parent.supportingMaterials == item.supportingMaterials
  }

  def discardDraft(user: OrgAndUser)(id: DraftId) = remove(user)(id)

  private def noVersion(i: Item) = i.copy(id = i.id.copy(version = None))

  protected def saveCommit(c: ItemCommit): Validation[DraftError, Unit] = {
    commitService.save(c).failed(SaveCommitFailed)
  }

  override def save(requester: OrgAndUser)(d: ItemDraft): Validation[DraftError, DraftId] = {
    if (draftService.owns(requester, d.id)) {
      draftService.save(d)
        .failed(e => SaveDataFailed(e.getErrorMessage))
        .map(_ => d.id)
    } else {
      Failure(UserCantSave(requester, d.user))
    }
  }

  override protected def copySrcToDraft(src: Item, draft: ItemDraft): Validation[DraftError, ItemDraft] = {
    val update = draft.copy(parent = ItemSrc(src), change = ItemSrc(src))
    for {
      _ <- draftService.save(update).failed(e => SaveDataFailed(e.getErrorMessage))
      _ <- assets.copyItemToDraft(src.id, draft.id)
    } yield update
  }

  override protected def copyDraftToSrc(d: ItemDraft): Validation[DraftError, ItemCommit] = {
    for {
      vid <- itemService.save(noVersion(d.change.data), false).disjunction.validation.leftMap { s => SaveDataFailed(s) }
      commit <- Success(ItemCommit(d.id, d.user, d.change.data.id))
      _ <- saveCommit(commit)
      _ <- assets.copyDraftToItem(d.id, commit.srcId)
    } yield commit
  }

  /**
   * Check that the draft src matches the latest src,
   * so that a commit is possible.
   */
  override def getLatestSrc(d: ItemDraft): Option[ItemSrc] = itemService.findOneById(d.parent.id.copy(version = None)).map(i => ItemSrc(i))

  private implicit class WriteResultToValidation(w: WriteResult) {
    require(w != null)

    def failed(e: DraftError): Validation[DraftError, Unit] = failed(_ => e)

    def failed(e: CommandResult => DraftError): Validation[DraftError, Unit] = if (w.getLastError.ok) {
      Success()
    } else {
      Failure(e(w.getLastError))
    }
  }


  override def draftIsOutOfDate(d: ItemDraft, src: Src[VersionedId[ObjectId], Item]): ItemDraftIsOutOfDate = {
    ItemDraftIsOutOfDate(d, src)
  }

}
