package org.corespring.web.common.controllers.deployment

import com.ee.assets.Loader
import org.corespring.common.log.PackageLogging
import play.api.Play

class AssetsLoader(val loader: Loader) extends PackageLogging {

  def init(implicit app: play.api.Application) = if (Play.isProd) {
    logger.debug("running S3 deployments...")
    tagger
    corespringCommon
    playerCommon
  }

  def tagger = loader.scripts("tagger")("js/corespring/tagger")

  def corespringCommon = loader.scripts("cs-common")(
    "js/corespring/common/services/ItemFormattingUtils.js",
    "js/corespring/common/services/MessageBridge.js",
    "js/corespring/common/directives/ResultPager.js",
    "js/corespring/common/directives/IframeAutoHeight.js",
    "js/corespring/common/services/Logger.js")

  def playerCommon = loader.scripts("common")(
    "js/corespring/qti/controllers",
    "js/corespring/qti/app.js",
    "js/corespring/qti/services/qtiServices.js",
    "js/corespring/qti/services/Canvas.js",
    "js/corespring/qti/directives/DimensionsChecker.js",
    "js/corespring/qti/directives/correctAnswer.js",
    "js/corespring/qti/directives/feedbackInline.js",
    "js/corespring/qti/prototype.extensions/Array.js",
    "js/corespring/qti/prototype.extensions/Function.js")
}

object AssetsLoaderImpl extends AssetsLoader(loader)
object LocalAssetsLoaderImpl extends AssetsLoader(localLoader)