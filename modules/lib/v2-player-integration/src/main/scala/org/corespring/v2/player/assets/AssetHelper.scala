package org.corespring.v2.player.assets

import org.apache.commons.httpclient.util.URIUtil
import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import play.api.mvc.SimpleResult

trait AssetHelper {

  def s3Service: S3Service

  def bucket: String

  protected def versionedIdFromString(itemService: ItemService, id: String): Option[VersionedId[ObjectId]] = {
    VersionedId(id).map { vid =>
      val version = vid.version.getOrElse(itemService.currentVersion(vid))
      VersionedId(vid.id, Some(version))
    }
  }

  protected def getAssetFromItemId(s3Path: String): SimpleResult = {
    s3Service.download(bucket, s3Path)
  }
}
