package org.corespring.drafts.item

import com.amazonaws.services.s3.AmazonS3Client
import org.bson.types.ObjectId
import org.corespring.container.client.AssetUtils
import org.corespring.drafts.errors._
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
