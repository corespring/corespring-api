package common.controllers.deployment

import play.api.{Play, Application}
import common.log.PackageLogging
import play.api.templates.Html

object AssetsLoader extends PackageLogging{

  def init(implicit app : Application) = if(Play.isProd){
    Logger.debug("running S3 deployments...")
    tagger
    corespringCommon
    playerCommon
  }

  def tagger: Html = tagger(false)
  def tagger(forceLocal: Boolean = false) = common.controllers.deployment.loader(forceLocal).scripts("tagger")("js/corespring/tagger")

  def corespringCommon: Html = corespringCommon(false)
  def corespringCommon(forceLocal: Boolean = false) = common.controllers.deployment.loader(forceLocal).scripts("cs-common")(
    "js/corespring/common/services/ItemFormattingUtils.js",
    "js/corespring/common/services/MessageBridge.js",
    "js/corespring/common/directives/ResultPager.js",
    "js/corespring/common/directives/IframeAutoHeight.js")


  def playerCommon: Html = playerCommon(false)
  def playerCommon(forceLocal: Boolean = false) = common.controllers.deployment.loader(forceLocal).scripts("common")(
    "js/corespring/qti/controllers",
    "js/corespring/qti/app.js",
    "js/corespring/qti/qtiServices.js",
    "js/corespring/qti/directives/DimensionsChecker.js",
    "js/corespring/qti/directives/feedbackInline.js",
    "js/corespring/qti/prototype.extensions/Array.js")
}
