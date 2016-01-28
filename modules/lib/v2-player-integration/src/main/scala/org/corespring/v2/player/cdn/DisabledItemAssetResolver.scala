package org.corespring.v2.player.cdn

import org.corespring.container.client.ItemAssetResolver

class DisabledItemAssetResolver extends ItemAssetResolver {

  override def resolve(itemId: String)(file: String): String = {
    file
  }

}
