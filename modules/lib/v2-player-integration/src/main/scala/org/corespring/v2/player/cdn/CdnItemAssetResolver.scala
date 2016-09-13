package org.corespring.v2.player.cdn

import org.corespring.common.url.EncodingHelper
import org.corespring.drafts.item.S3Paths
import org.corespring.platform.data.mongo.models.VersionedId

class CdnItemAssetResolver(cdnResolver: CdnResolver) extends ItemAssetResolver {

  val helper = new EncodingHelper

  override def resolve(itemId: String)(file: String): String = {
    cdnResolver.resolveDomain(mkPath(itemId)(helper.encodedOnce(file)))
  }

}
