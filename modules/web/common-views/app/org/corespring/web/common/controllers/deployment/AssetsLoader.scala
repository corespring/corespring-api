package org.corespring.web.common.controllers.deployment

import com.amazonaws.services.s3.AmazonS3
import com.ee.assets.Loader
import com.ee.assets.deployment.Deployer
import com.google.javascript.jscomp.CompilerOptions
import org.apache.commons.lang3.StringUtils
import org.corespring.web.common.views.helpers.BuildInfo
import play.api.Mode.Mode
import play.api.{ Configuration, Mode, Logger }

private[deployment] object ValidBucketName {

  val bucketNameLengthMax = 63
  val base = "corespring-public-assets"

  def apply(env: String, branch: String): String = {
    val bucketCompliantBranchName = branch.replaceAll("feature", "").replaceAll("hotfix", "").replaceAll("/", "-")
    val raw = Seq(base, env, bucketCompliantBranchName).filterNot(_.isEmpty).mkString("-").toLowerCase.replaceAll("--", "-")
    val trimmed = raw.take(bucketNameLengthMax)
    if (trimmed.endsWith("-")) {
      StringUtils.substringBeforeLast(trimmed, "-")
    } else {
      trimmed
    }
  }
}

class AssetsLoader(mode: Mode, config: Configuration, s3Client: AmazonS3, buildInfo: BuildInfo) {

  private[this] val logger = Logger(classOf[AssetsLoader])

  private def isProd: Boolean = mode == Mode.Prod

  lazy val closureOptions = {
    val o = new CompilerOptions()
    o.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT5)
    o
  }

  lazy val loader: Loader = if (isProd) {

    lazy val branch: String = if (buildInfo.branch.isEmpty || buildInfo.branch == "?") "no-branch" else buildInfo.branch

    val commitHash = buildInfo.commitHashShort

    lazy val releaseRoot: String = if (commitHash.isEmpty || commitHash == "?") {
      val format = new java.text.SimpleDateFormat("yyyy-MM-dd--HH-mm")
      "dev-" + format.format(new java.util.Date())
    } else {
      commitHash
    }

    val bucketName: String = {
      if (isProd) {
        val envName = config.getString("ENV_NAME").getOrElse("")
        ValidBucketName(envName, branch)
      } else {
        "corespring-dev-tmp-assets"
      }
    }

    lazy val s3Deployer: Deployer = new S3Deployer(Some(s3Client), bucketName, releaseRoot)
    new Loader(Some(s3Deployer), mode, config, Some(closureOptions))
  } else {
    new Loader(None, mode, config, Some(closureOptions))
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

  def init() = if (isProd) {
    logger.debug("running S3 deployments...")
    tagger
    corespringCommon
    playerCommon
  }

  init()
}

