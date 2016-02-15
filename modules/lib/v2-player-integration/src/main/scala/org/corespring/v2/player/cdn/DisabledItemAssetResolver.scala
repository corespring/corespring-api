package org.corespring.v2.player.cdn

class DisabledItemAssetResolver extends ItemAssetResolver {

  override def resolve(itemId: String)(file: String): String = {
    file
  }

}
