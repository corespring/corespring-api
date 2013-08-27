package org.corespring.web.common.controllers.deployment

import com.ee.assets.Loader
import org.corespring.common.log.PackageLogging
import play.api.Play

class AssetsLoader(val loader: Loader) extends PackageLogging {

  def init(implicit app: play.api.Application) = if (Play.isProd) {
    Logger.debug("running S3 deployments...")
    tagger
    corespringCommon
    playerCommon
  }

  def tagger = loader.scripts("tagger")("js/corespring/tagger")

  def corespringCommon = loader.scripts("cs-common")(
    "js/corespring/common/services/ItemFormattingUtils.js",
    "js/corespring/common/services/MessageBridge.js",
    "js/corespring/common/directives/ResultPager.js",
    "js/corespring/common/directives/IframeAutoHeight.js")

  def playerCommon = loader.scripts("common")(
    "js/corespring/qti/controllers",
    "js/corespring/qti/app.js",
    "js/corespring/qti/qtiServices.js",
    "js/corespring/qti/directives/DimensionsChecker.js",
    "js/corespring/qti/directives/feedbackInline.js",
    "js/corespring/qti/prototype.extensions/Array.js",
    "js/corespring/qti/prototype.extensions/Function.js")
}

object AssetsLoaderImpl extends AssetsLoader(loader)
object LocalAssetsLoaderImpl extends AssetsLoader(localLoader)