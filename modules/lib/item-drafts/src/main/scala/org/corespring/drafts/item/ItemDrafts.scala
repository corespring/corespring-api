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
 * An implementation of <DraftsWithCommitAndCreate> for <Item> backed by some mongo services.
 */
trait ItemDrafts
  extends DraftsWithCommitAndCreate[ObjectId, ObjectId, Long, Item, SimpleUser, ItemDraft, ItemCommit] {

  def itemService: ItemService

  def draftService: ItemDraftService

  def commitService: CommitService

  import Helpers._

  def list(id : VersionedId[ObjectId]) : Seq[ItemDraft] = {
    val version = id.version.getOrElse(itemService.currentVersion(id))
    draftService.findByIdAndVersion(id.id, version)
  }

  override def load(id: ObjectId): Option[ItemDraft] = draftService.load(id)

  override def save(d: ItemDraft): Validation[DraftError, ObjectId] = {
    val result = draftService.save(d)
    if (result.getLastError.ok) {
      Success(d.id)
    } else {
      Failure(SaveDataFailed(result.getLastError.getErrorMessage))
    }
  }

  def removeUserDraft(id:ObjectId, user : SimpleUser) : Validation[DraftError,ObjectId]= {
    val result = draftService.removeUserDraft(id, user)
    if(result.getN != 1){
      Failure(DeleteFailed)
    } else {
      Success(id)
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
