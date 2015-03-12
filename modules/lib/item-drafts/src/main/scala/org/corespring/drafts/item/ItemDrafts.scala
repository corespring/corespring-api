package org.corespring.drafts.item

import org.bson.types.ObjectId
import org.corespring.drafts.item.models.{ SimpleUser, ItemCommit, ItemSrc, ItemDraft }
import org.corespring.drafts.item.services.{ ItemDraftService, CommitService }
import org.corespring.drafts.{ Commit, IdAndVersion, DraftsWithCommitAndCreate }
import org.corespring.drafts.errors._
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId

import scalaz.{ Failure, Success, Validation }

/**
 * Commit clean up ...
 *
 * edDraft of 1:10
 * gwenDraft of 1:10
 *
 * commit edDraft --> src: 1:10, committed: 1:11
 *
 * find all drafts that target id: 1, list their versions: 8, 9, 10
 * remove all commits with id: 1 and version not in 8,9,10
 *
 * commit gwenDraft - check commits for 1:10 if count > 0 fail
 *
 */
trait ItemDrafts
  extends DraftsWithCommitAndCreate[ObjectId, ObjectId, Long, Item, SimpleUser, ItemDraft, ItemCommit] {

  def itemService: ItemService

  def draftService: ItemDraftService

  def commitService: CommitService

  import Helpers._

  override def load(id: ObjectId): Option[ItemDraft] = draftService.load(id)

  override def save(d: ItemDraft): Validation[DraftError, ObjectId] = {
    val result = draftService.save(d)
    if (result.getLastError.ok) {
      Success(d.id)
    } else {
      Failure(SaveDataFailed(result.getLastError.getErrorMessage))
    }
  }

  override def loadCommits(idAndVersion: IdAndVersion[ObjectId, Long]): Seq[Commit[ObjectId, Long, SimpleUser]] = {
    commitService.findByIdAndVersion(idAndVersion.id, idAndVersion.version)
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

  override protected def mkDraft(srcId: ObjectId, src: Item, user: SimpleUser): ItemDraft = ItemDraft(ObjectId.get, ItemSrc(src, src.id), user)

  override protected def deleteDraft(d: ItemDraft): Validation[DraftError, Unit] = {
    val result = draftService.remove(d)
    if (result) Success() else Failure(DeleteDraftFailed(d.id))
  }
}
