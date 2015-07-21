package org.corespring.web.common.controllers.deployment

import com.ee.assets.Loader
import org.corespring.web.common.views.helpers.Defaults
import play.api.{Mode, Configuration, Logger, Play}

class AssetsLoader(val loader: Loader, defaults : Defaults, config:Configuration) {

  val logger = Logger(classOf[AssetsLoader])

  private def isProd: Boolean = play.api.Play.current.mode == Mode.Prod

  //lazy val loader: Loader = new Loader(if (isProd) Some(s3Deployer) else None, Play.mode, config)
  //lazy val localLoader: Loader = new Loader(None, Play.mode, config)

  //lazy val s3Deployer: Deployer = new S3Deployer(Some(CorespringS3ServiceExtended.getClient), bucketName, releaseRoot)

  val bucketName: String = {
    val publicAssets = "corespring-public-assets"
    if (isProd) {
      val envName = defaults.envName("")
      val bucketCompliantBranchName = branch.replaceAll("/", "-")
      Seq(publicAssets, envName, bucketCompliantBranchName).filterNot(_.isEmpty).mkString("-").toLowerCase
    } else {
      "corespring-dev-tmp-assets"
    }
  }

  lazy val branch: String = if (defaults.branch.isEmpty || defaults.branch == "?") "no-branch" else defaults.branch

  lazy val releaseRoot: String = if (defaults.commitHashShort.isEmpty || defaults.commitHashShort == "?") {
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd--HH-mm")
    "dev-" + format.format(new java.util.Date())
  } else {
    defaults.commitHashShort
  }
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

