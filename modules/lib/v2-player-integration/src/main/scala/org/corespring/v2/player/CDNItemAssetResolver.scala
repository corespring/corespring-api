package org.corespring.v2.player

import org.corespring.container.client.ItemAssetResolver

class CDNItemAssetResolver( cdnResolver: CDNResolver) extends ItemAssetResolver  {

  override def resolve(itemId:String)(file:String):String = {
    cdnResolver.resolveDomain(super.resolve(itemId)(file))
  }
}
