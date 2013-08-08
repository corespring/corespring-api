package scorm.controllers

import common.controllers.utils.BaseUrl
import org.corespring.platform.core.models.item.service.ItemServiceImpl
import play.api.mvc.{AnyContent, Request}
import player.controllers.{AssetLoading => PlayerAssetLoading, AssetLoadingDefaults}
import org.corespring.common.encryption.AESCrypto

object AssetLoading extends PlayerAssetLoading(
  AESCrypto,
  AssetLoadingDefaults.Templates.player,
  ItemServiceImpl,
  AssetLoadingDefaults.ErrorHandler.handleError){
  override protected def getBaseUrl(r:Request[AnyContent]) : String = BaseUrl(r) + "/scorm-player"
}
