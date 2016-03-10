package org.corespring.v2.player.assets

import org.corespring.amazon.s3.S3Service
import org.corespring.drafts.item.S3Paths
import org.corespring.models.appConfig.Bucket
import org.corespring.services.item.ItemService
import org.corespring.v2.player.hooks.CatalogAssets
import play.api.mvc.{ AnyContent, Request }

class CatalogAssetHelper(itemService: ItemService, val s3Service: S3Service, val bucketConfig: Bucket) extends CatalogAssets with AssetHelper with ResultHeaders {

  override def loadFile(id: String, path: String)(request: Request[AnyContent]) =
    versionedIdFromString(itemService, id).map { vid =>
      getAssetFromItemId(S3Paths.itemFile(vid, path)).withContentHeaders(path)
    }.getOrElse(play.api.mvc.Results.BadRequest(s"Invalid versioned id: $id"))

  override def bucket: String = bucketConfig.bucket
}

