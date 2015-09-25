package org.corespring.web.common.controllers.deployment

import com.ee.assets.Loader
import com.ee.assets.deployment.Deployer
import org.apache.commons.lang3.StringUtils
import org.corespring.assets.CorespringS3ServiceExtended
import org.corespring.web.common.views.helpers.BuildInfo
import play.api.{Mode, Logger, Play}

object AssetsLoader {

  import play.api.Play.current

  private[this] val logger = Logger(this.getClass())

  private val configuration = Play.current.configuration
  private def isProd: Boolean = play.api.Play.current.mode == Mode.Prod

  val bucketNameLengthMax = 63

  lazy val loader: Loader = if(isProd) {

    lazy val branch: String = if (BuildInfo.branch.isEmpty || BuildInfo.branch == "?") "no-branch" else BuildInfo.branch

    lazy val releaseRoot: String = if (BuildInfo.commitHashShort.isEmpty || BuildInfo.commitHashShort == "?") {
      val format = new java.text.SimpleDateFormat("yyyy-MM-dd--HH-mm")
      "dev-" + format.format(new java.util.Date())
    } else {
      BuildInfo.commitHashShort
    }


    val bucketName: String = {
      val publicAssets = "corespring-public-assets"
      if (isProd) {
        val envName = configuration.getString("ENV_NAME").getOrElse("")
        val bucketCompliantBranchName = branch.replaceAll("feature", "").replaceAll("hotfix", "").replaceAll("/", "-")
        val raw = Seq(publicAssets, envName, bucketCompliantBranchName).filterNot(_.isEmpty).mkString("-").toLowerCase
        val trimmed = raw.take(bucketNameLengthMax)
        StringUtils.substringBeforeLast(trimmed, "-")
      } else {
        "corespring-dev-tmp-assets"
      }
    }

    lazy val s3Deployer: Deployer = new S3Deployer(Some(CorespringS3ServiceExtended.getClient), bucketName, releaseRoot)
    new Loader(Some(s3Deployer), Play.mode, configuration)
  } else {
    new Loader(None, Play.mode, configuration)
  }


  def init(implicit app: play.api.Application) = if (isProd) {
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

