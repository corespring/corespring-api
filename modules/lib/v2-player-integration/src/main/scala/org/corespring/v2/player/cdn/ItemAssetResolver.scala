package org.corespring.v2.player.cdn

import play.api.mvc.RequestHeader

trait ItemAssetResolver {

  def bypass(header: RequestHeader): Boolean = true

  def resolve(itemId: String)(file: String): String = {
    mkPath(itemId)(file)
  }

  protected def mkPath(itemId: String)(imageSrc: String): String = {
    val plainImageSrc = imageSrc.split('/').last
    val playerRoutes = org.corespring.container.client.controllers.apps.routes.Player
    playerRoutes.getFileByItemId(itemId, plainImageSrc).url
  }
}
