package scorm.controllers

import common.controllers.utils.BaseUrl
import play.api.mvc.{ AnyContent, Request }
import player.controllers.{ AssetLoading => PlayerAssetLoading, AssetLoadingDefaults }
import org.corespring.common.encryption.AESCrypto
import org.corespring.platform.core.services.item.ItemServiceImpl

object AssetLoading extends PlayerAssetLoading(
  AESCrypto,
  AssetLoadingDefaults.Templates.player,
  ItemServiceImpl,
  AssetLoadingDefaults.ErrorHandler.handleError) {
  override protected def getBaseUrl(r: Request[AnyContent]): String = BaseUrl(r) + "/scorm-player"
}
