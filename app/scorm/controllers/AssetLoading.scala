package scorm.controllers

import org.corespring.common.encryption.AESCrypto
import org.corespring.common.url.BaseUrl
import org.corespring.platform.core.services.item.ItemServiceImpl
import org.corespring.player.v1.controllers.AssetLoadingDefaults
import play.api.mvc.{ AnyContent, Request }

object AssetLoading extends org.corespring.player.v1.controllers.AssetLoading(
  AESCrypto,
  AssetLoadingDefaults.Templates.player,
  ItemServiceImpl,
  AssetLoadingDefaults.ErrorHandler.handleError) {
  override protected def getBaseUrl(r: Request[AnyContent]): String = BaseUrl(r) + "/scorm-player"
}
