package scorm.controllers

import play.api.mvc.{AnyContent, Request}
import common.controllers.utils.BaseUrl
import player.controllers.{AssetLoading => PlayerAssetLoading, DefaultTemplate}
import common.encryption.AESCrypto
import models.item.service.ItemServiceImpl

object AssetLoading extends PlayerAssetLoading(AESCrypto,DefaultTemplate.player, ItemServiceImpl){
  override protected def getBaseUrl(r:Request[AnyContent]) : String = BaseUrl(r) + "/scorm-player"
}
