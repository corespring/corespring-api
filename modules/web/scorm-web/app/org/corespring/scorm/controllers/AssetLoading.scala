package org.corespring.scorm.controllers

import org.corespring.common.encryption.AESCrypto
import org.corespring.common.url.BaseUrl
import org.corespring.platform.core.services.item.ItemServiceWired
import play.api.mvc.{ AnyContent, Request }
import org.corespring.player.v1.controllers.launcher.{AssetLoading => MainAssetLoading, AssetLoadingDefaults}

object AssetLoading extends MainAssetLoading(
  AESCrypto,
  AssetLoadingDefaults.Templates.player,
  ItemServiceWired,
  AssetLoadingDefaults.ErrorHandler.handleError) {
  override protected def getBaseUrl(r: Request[AnyContent]): String = BaseUrl(r) + "/scorm/player"
}
