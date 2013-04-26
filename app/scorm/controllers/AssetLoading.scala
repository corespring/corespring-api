package scorm.controllers

import play.api.mvc.{AnyContent, Request}
import common.controllers.utils.BaseUrl
import player.controllers.{DefaultTemplate, AssetLoading}
import common.encryption.AESCrypto

object AssetLoading extends AssetLoading(AESCrypto,DefaultTemplate.player){
  override protected def getBaseUrl(r:Request[AnyContent]) : String = BaseUrl(r) + "/scorm-player"
}
