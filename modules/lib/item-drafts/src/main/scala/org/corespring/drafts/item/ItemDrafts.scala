package org.corespring.drafts.item

import org.bson.types.ObjectId
import org.corespring.drafts.item.models.{ SimpleUser, ItemCommit, ItemSrc, ItemDraft }
import org.corespring.drafts.item.services.{ ItemDraftService, CommitService }
import org.corespring.drafts.{ Commit, IdAndVersion, DraftsWithCommitCheck }
import org.corespring.drafts.errors.{ SaveDataFailed, DraftError }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId

trait ItemDrafts
  extends DraftsWithCommitCheck[ObjectId, ObjectId, Long, Item, SimpleUser, ItemDraft] {

  def itemService: ItemService

  def draftService: ItemDraftService

  def commitService: CommitService

  import Helpers._

  /**
   * Creates a draft for the target data.
   */
  override def create(id: ObjectId, user: SimpleUser): Option[ItemDraft] = {
    itemService.findOneById(VersionedId(id, None)).flatMap { i =>
      val draft = ItemDraft(ObjectId.get, ItemSrc(i, i.id), user)
      val saveResult = draftService.save(draft)
      if (saveResult.getLastError.ok) {
        Some(draft)
      } else {
        None
      }
    }
  }

  override def load(id: ObjectId): Option[ItemDraft] = draftService.load(id)

  override def save(d: ItemDraft): Either[DraftError, ObjectId] = {
    val result = draftService.save(d)
    if (result.getLastError.ok) {
      Right(d.id)
    } else {
      Left(SaveDataFailed(result.getLastError.getErrorMessage))
    }
  }

  /**
   * Load commits that have used the same srcId
   * @return
   */
  override def loadCommits(idAndVersion: IdAndVersion[ObjectId, Long]): Seq[Commit[ObjectId, Long, SimpleUser]] = {
    commitService.findByIdAndVersion(idAndVersion.id, idAndVersion.version)
  }

  /**
   * commit the draft, create a commit and store it
   * for future checks.
   * @param d
   * @return
   */
  override def commitData(d: ItemDraft): Either[DraftError, Commit[ObjectId, Long, SimpleUser]] = {
    itemService.save(d.src.data, true) match {
      case Left(err) => Left(SaveDataFailed(err))
      case Right(vid) => {
        val commit = ItemCommit(d.src.data.id, vid, d.user)
        commitService.save(commit)
        draftService.remove(d)
        Right(commit)
      }
    }
  }
}
