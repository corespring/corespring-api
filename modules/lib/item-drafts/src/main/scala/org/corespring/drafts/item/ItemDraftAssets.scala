package org.corespring.drafts.item

import com.amazonaws.services.s3.AmazonS3
import org.bson.types.ObjectId
import org.corespring.assets.AssetUtils
import org.corespring.assets.{ ItemAssetKeys, AssetKeys }
import org.corespring.drafts.errors._
import org.corespring.drafts.item.models.DraftId
import org.corespring.models.appConfig.Bucket
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.Logger

import scalaz.{ Failure, Success, Validation }

trait ItemDraftAssets {
  def copyItemToDraft(itemId: VersionedId[ObjectId], draftId: DraftId): Validation[DraftError, DraftId]
  def copyDraftToItem(draftId: DraftId, itemId: VersionedId[ObjectId]): Validation[DraftError, VersionedId[ObjectId]]
  def deleteDraft(draftId: DraftId): Validation[DraftError, Unit]
  def deleteDrafts(draftId: DraftId*): Seq[Validation[DraftError, Unit]]
  def deleteDraftsByItemId(itemId: ObjectId): Validation[DraftError, Unit]
}

object DraftAssetKeys extends AssetKeys[DraftId] {

  override def folder(id: DraftId): String = {
    s"${draftItemIdFolder(id.itemId)}/org-${id.orgId}/${id.name}"
  }

  def draftItemIdFolder(itemId: ObjectId) = s"item-drafts/item-${itemId}"
}

object S3Paths {

  lazy val logger = Logger(S3Paths.getClass)

  def itemFolder(id: VersionedId[ObjectId]) = ItemAssetKeys.folder(id)

  def itemFile(id: VersionedId[ObjectId], path: String): String = ItemAssetKeys.file(id, path)

  def draftFolder(id: DraftId): String = DraftAssetKeys.folder(id)

  def draftFile(id: DraftId, path: String): String = DraftAssetKeys.file(id, path)
}

class S3ItemDraftAssets(s3: AmazonS3, bucket: Bucket) extends ItemDraftAssets {

  lazy val logger = Logger(classOf[S3ItemDraftAssets])

  lazy val utils = new AssetUtils(s3, bucket.bucket)

  private def cp[A](from: String, to: String, id: A): Validation[DraftError, A] = {
    logger.debug(s"function=cp from=$from to=$to id=$id")

    utils.copyDir(from, to) match {
      case true => Success(id)
      case false => Failure(CopyAssetsFailed(from, to))
    }
  }

  override def copyItemToDraft(itemId: VersionedId[ObjectId], draftId: DraftId): Validation[DraftError, DraftId] = {
    val from = S3Paths.itemFolder(itemId)
    val to = S3Paths.draftFolder(draftId)
    cp[DraftId](from, to, draftId)
  }

  override def deleteDraft(draftId: DraftId): Validation[DraftError, Unit] = {
    val path = S3Paths.draftFolder(draftId)
    utils.deleteDir(path) match {
      case true => Success(Unit)
      case false => Failure(DeleteAssetsFailed(path))
    }
  }

  override def deleteDrafts(ids: DraftId*): Seq[Validation[DraftError, Unit]] = {
    ids.map { deleteDraft }
  }

  override def deleteDraftsByItemId(itemId: ObjectId): Validation[DraftError, Unit] = {
    import scala.language.reflectiveCalls
    val path = DraftAssetKeys.draftItemIdFolder(itemId)
    utils.deleteDir(path) match {
      case true => Success(Unit)
      case false => Failure(DeleteAssetsFailed(path))
    }
  }

  override def copyDraftToItem(draftId: DraftId, itemId: VersionedId[ObjectId]): Validation[DraftError, VersionedId[ObjectId]] = {
    val from = S3Paths.draftFolder(draftId)
    val to = S3Paths.itemFolder(itemId)
    cp[VersionedId[ObjectId]](from, to, itemId)
  }

}
