package org.corespring.v2.player

import org.corespring.container.client.ItemAssetResolver

import org.corespring.drafts.item.S3Paths
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.DateTime

class UnsignedItemAssetResolver(
  cdnResolver: CDNResolver) extends ItemAssetResolver {

  override def resolve(itemId: String)(file: String): String = {
    cdnResolver.resolveDomain(s3ObjectPath(itemId, file))
  }

  protected def s3ObjectPath(itemId: String, file: String) = {
    val vid = VersionedId(itemId).getOrElse(throw new IllegalArgumentException(s"Invalid itemId: $itemId"))
    S3Paths.itemFile(vid, file)
  }

}
