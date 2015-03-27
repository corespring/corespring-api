package org.corespring.drafts.item

import com.amazonaws.services.s3.AmazonS3Client
import org.bson.types.ObjectId
import org.corespring.container.client.AssetUtils
import org.corespring.drafts.item.models.{ OrgAndUser, ItemCommit, ItemSrc, ItemDraft }
import org.corespring.drafts.item.services.{ ItemDraftService, CommitService }
import org.corespring.drafts.{ Commit, IdAndVersion, DraftsWithCommitAndCreate }
import org.corespring.drafts.errors._
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId

import scalaz.{ Failure, Success, Validation }

trait ItemDraftAssets {
  def copyItemToDraft(itemId: VersionedId[ObjectId], draftId: ObjectId): Validation[DraftError, ObjectId]
  def copyDraftToItem(draftId: ObjectId, itemId: VersionedId[ObjectId]): Validation[DraftError, VersionedId[ObjectId]]
  def deleteDraft(draftId: ObjectId): Validation[DraftError, Unit]
}

trait S3ItemDraftAssets extends ItemDraftAssets {
  def s3: AmazonS3Client

  def bucket: String

  def utils = new AssetUtils(s3, bucket)

  private def draftPath(id: ObjectId) = s"item-drafts/$id"
  private def itemPath(id: VersionedId[ObjectId]) = {
    id.version.map { v =>
      s"items/${id.id}/$v"
    }.getOrElse {
      throw new IllegalArgumentException(s"VersionedId must have a version defined")
    }
  }

  private def cp[A](from: String, to: String, id: A): Validation[DraftError, A] = utils.copyDir(from, to) match {
    case true => Success(id)
    case false => Failure(CopyAssetsFailed(from, to))
  }

  override def copyItemToDraft(itemId: VersionedId[ObjectId], draftId: ObjectId): Validation[DraftError, ObjectId] = {
    val from = itemPath(itemId)
    val to = draftPath(draftId)
    cp[ObjectId](from, to, draftId)
  }

  override def deleteDraft(draftId: ObjectId): Validation[DraftError, Unit] = {
    val path = s"item-drafts/$draftId/"
    utils.deleteDir(path) match {
      case true => Success(Unit)
      case false => Failure(DeleteAssetsFailed(path))
    }
  }

  override def copyDraftToItem(draftId: ObjectId, itemId: VersionedId[ObjectId]): Validation[DraftError, VersionedId[ObjectId]] = {
    val from = draftPath(draftId)
    val to = itemPath(itemId)
    cp[VersionedId[ObjectId]](from, to, itemId)
  }
}

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

  override def load(id: ObjectId): Option[ItemDraft] = draftService.load(id)

  override def save(d: ItemDraft): Validation[DraftError, ObjectId] = {
    val result = draftService.save(d)
    if (result.getLastError.ok) {
      Success(d.id)
    } else {
      Failure(SaveDataFailed(result.getLastError.getErrorMessage))
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

  override def commit(d: ItemDraft, force: Boolean = false): Validation[DraftError, ItemCommit] = {
    super.commit(d, force).flatMap { c =>
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
