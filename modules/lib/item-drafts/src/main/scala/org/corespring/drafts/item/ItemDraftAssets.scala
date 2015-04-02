package org.corespring.drafts.item

import com.amazonaws.services.s3.AmazonS3Client
import org.bson.types.ObjectId
import org.corespring.container.client.AssetUtils
import org.corespring.drafts.errors._
import org.corespring.drafts.item.models.ItemDraft
import org.corespring.platform.data.mongo.models.VersionedId

import scalaz.{ Failure, Success, Validation }
trait ItemDraftAssets {
  def copyItemToDraft(itemId: VersionedId[ObjectId], draftId: ObjectId): Validation[DraftError, ObjectId]
  def copyDraftToItem(draftId: ObjectId, itemId: VersionedId[ObjectId]): Validation[DraftError, VersionedId[ObjectId]]
  def deleteDraft(draftId: ObjectId): Validation[DraftError, Unit]
}

/**
 * Note: for now it is safe to assume that the asset will be located in the 'data' folder,
 * because assets are disabled in supporting materials.
 * When it comes to adding that back in we'll need to change this.
 * I'm proposing that the client uses the appropriate relative path,
 * eg:
 *   data/img.png
 *   supporting-materials/name/img.png
 * @see PE-98
 */
object S3Paths {

  def itemFolder(id: VersionedId[ObjectId]) = itemFromStringId(id.toString)

  def itemFile(id: String, path: String): String = s"${itemFromStringId(id)}/data/$path"

  def itemSupportingMaterialFile(id: String, supportingMaterial: String, path: String): String = {
    s"${itemFromStringId(id)}/supporting-materials/$supportingMaterial/$path"
  }

  def itemFromStringId(id: String): String = {
    require(id.contains(":"), s"version must be defined: $id")
    id.replace(":", "/")
  }

  def draftFolder(id: ObjectId): String = s"item-drafts/$id"

  def draftFile(id: ObjectId, path: String): String = s"${draftFolder(id)}/data/$path"

  def draftSupportingMaterialFile(id: ObjectId, supportingMaterial: String, path: String): String = {
    s"${draftFolder(id)}/supporting-materials/$supportingMaterial/$path"
  }
}

trait S3ItemDraftAssets extends ItemDraftAssets {
  def s3: AmazonS3Client

  def bucket: String

  def utils = new AssetUtils(s3, bucket)

  private def cp[A](from: String, to: String, id: A): Validation[DraftError, A] = utils.copyDir(from, to) match {
    case true => Success(id)
    case false => Failure(CopyAssetsFailed(from, to))
  }

  override def copyItemToDraft(itemId: VersionedId[ObjectId], draftId: ObjectId): Validation[DraftError, ObjectId] = {
    val from = S3Paths.itemFolder(itemId)
    val to = S3Paths.draftFolder(draftId)
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
    val from = S3Paths.draftFolder(draftId)
    val to = S3Paths.itemFolder(itemId)
    cp[VersionedId[ObjectId]](from, to, itemId)
  }
}
