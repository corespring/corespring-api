package org.corespring.v2.player.cdn

import org.corespring.drafts.item.S3Paths
import org.corespring.platform.data.mongo.models.VersionedId

class CdnItemAssetResolver(cdnResolver: CdnResolver) extends ItemAssetResolver {

  override def resolve(itemId: String)(file: String): String = {
    cdnResolver.resolveDomain(mkPath(itemId)(file))
  }

}
