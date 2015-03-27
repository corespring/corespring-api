package org.corespring.drafts.item

import org.bson.types.ObjectId
import org.corespring.drafts.errors._
import org.corespring.drafts.item.models.{ItemCommit, ItemDraft, ItemSrc, OrgAndUser}
import org.corespring.drafts.item.services.{CommitService, ItemDraftService}
import org.corespring.drafts.{Commit, DraftsWithCommitAndCreate, IdAndVersion}
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId

import scalaz.{Failure, Success, Validation}


/**
 * An implementation of <DraftsWithCommitAndCreate> for <Item> backed by some mongo services.
 */
trait ItemDrafts
  extends DraftsWithCommitAndCreate[ObjectId, ObjectId, Long, Item, OrgAndUser, ItemDraft, ItemCommit] {

  def itemService: ItemService

  def draftService: ItemDraftService

  def commitService: CommitService

  def assets: ItemDraftAssets

  import Helpers._

  def listForOrg(orgId: ObjectId) = draftService.listForOrg(orgId)

  def list(id: VersionedId[ObjectId]): Seq[ItemDraft] = {
    val version = id.version.getOrElse(itemService.currentVersion(id))
    draftService.findByIdAndVersion(id.id, version)
  }

  override def load(requester: OrgAndUser)(id: ObjectId): Option[ItemDraft] = {
    draftService.load(id).filter { d => d.user == requester }
  }

  def collection = draftService.collection

  def owns(requester: OrgAndUser)(id: ObjectId): Boolean = draftService.owns(requester, id)

  override def save(requester: OrgAndUser)(d: ItemDraft): Validation[DraftError, ObjectId] = {
    if (d.user == requester) {
      val result = draftService.save(d)
      if (result.getLastError.ok) {
        Success(d.id)
      } else {
        Failure(SaveDataFailed(result.getLastError.getErrorMessage))
      }
    } else {
      Failure(UserCantSave(requester, d.user))
    }
  }

  def removeDraftByIdAndUser(id: ObjectId, user: OrgAndUser): Validation[DraftError, ObjectId] = {
    for {
      result <- Success(draftService.removeDraftByIdAndUser(id, user))
      draftId <- if (result.getN != 1) Failure(DeleteFailed) else Success(id)
      deleteComplete <- assets.deleteDraft(draftId)
    } yield draftId
  }

  override def loadCommits(idAndVersion: IdAndVersion[ObjectId, Long]): Seq[Commit[ObjectId, Long, OrgAndUser]] = {
    commitService.findByIdAndVersion(idAndVersion.id, idAndVersion.version)
  }

  override def commit(requester: OrgAndUser)(d: ItemDraft, force: Boolean = false): Validation[DraftError, ItemCommit] = {
    super.commit(requester)(d, force).flatMap { c =>
      val copyResult = assets.copyDraftToItem(d.id, d.src.data.id)
      copyResult.map { _ => c }
    }
  }

  override protected def saveCommit(c: ItemCommit): Validation[CommitError, Unit] = {
    val result = commitService.save(c)
    if (result.getLastError.ok) {
      Success()
    } else {
      Failure(SaveCommitFailed)
    }
  }

  override protected def saveDraftSrcAsNewVersion(d: ItemDraft): Validation[DraftError, ItemCommit] = {
    itemService.save(d.src.data.copy(id = d.src.data.id.copy(version = None)), true) match {
      case Left(err) => Failure(SaveDataFailed(err))
      case Right(vid) => Success(ItemCommit(d.src.data.id, vid, d.user))
    }
  }

  override protected def findLatestSrc(id: ObjectId): Option[Item] = itemService.findOneById(VersionedId(id, None))

  override protected def mkDraft(srcId: ObjectId, src: Item, user: OrgAndUser): Validation[DraftError, ItemDraft] = {
    assets.copyItemToDraft(src.id, ObjectId.get).map { oid =>
      ItemDraft(oid, ItemSrc(src, src.id), user)
    }
  }

  override protected def deleteDraft(d: ItemDraft): Validation[DraftError, Unit] = for {
    result <- Success(draftService.remove(d))
    _ <- if (result) Success() else Failure(DeleteDraftFailed(d.id))
    deleteComplete <- assets.deleteDraft(d.id)
  } yield Unit

}
