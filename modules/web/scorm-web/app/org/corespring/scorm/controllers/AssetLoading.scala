package org.corespring.scorm.controllers

import org.corespring.common.encryption.AESCrypto
import org.corespring.common.url.BaseUrl
import org.corespring.platform.core.services.item.ItemServiceImpl
import play.api.mvc.{ AnyContent, Request }
import org.corespring.player.v1.controllers.launcher.{AssetLoading => MainAssetLoading, AssetLoadingDefaults}

object AssetLoading extends MainAssetLoading(
  AESCrypto,
  AssetLoadingDefaults.Templates.player,
  ItemServiceImpl,
  AssetLoadingDefaults.ErrorHandler.handleError) {
  override protected def getBaseUrl(r: Request[AnyContent]): String = BaseUrl(r) + "/scorm/player"
}
