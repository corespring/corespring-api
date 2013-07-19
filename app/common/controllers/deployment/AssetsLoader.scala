package common.controllers.deployment

import play.api.{Play, Application}
import common.log.PackageLogging

object AssetsLoader extends PackageLogging{

  def init(implicit app : Application) = if(Play.isProd){
    Logger.debug("running S3 deployments...")
    tagger
    corespringCommon
    playerCommon
  }

  def tagger = common.controllers.deployment.loader.scripts("tagger")("js/corespring/tagger")

  def corespringCommon = common.controllers.deployment.loader.scripts("cs-common")(
    "js/corespring/common/services/ItemFormattingUtils.js",
    "js/corespring/common/services/MessageBridge.js",
    "js/corespring/common/directives/ResultPager.js",
    "js/corespring/common/directives/IframeAutoHeight.js")


  def playerCommon = common.controllers.deployment.loader.scripts("common")(
    "js/corespring/qti/controllers",
    "js/corespring/qti/app.js",
    "js/corespring/qti/qtiServices.js",
    "js/corespring/qti/directives/DimensionsChecker.js",
    "js/corespring/qti/directives/feedbackInline.js",
    "js/corespring/qti/prototype.extensions/Array.js",
    "js/corespring/qti/prototype.extensions/Function.js")
}
