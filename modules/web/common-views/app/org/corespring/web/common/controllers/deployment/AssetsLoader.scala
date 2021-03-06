package org.corespring.web.common.controllers.deployment

import com.amazonaws.services.s3.AmazonS3
import com.ee.assets.Loader
import com.ee.assets.deployment.Deployer
import com.google.javascript.jscomp.CompilerOptions
import org.apache.commons.lang3.StringUtils
import play.api.Mode.Mode
import play.api.{ Configuration, Mode, Logger }

case class BranchAndHash(branch: String, hash: String)

class AssetsLoader(mode: Mode, config: Configuration, s3Client: AmazonS3, branchAndHash: BranchAndHash) {

  private[this] val logger = Logger(classOf[AssetsLoader])

  private def isProd: Boolean = mode == Mode.Prod

  val bucketNameLengthMax = 63

  lazy val closureOptions = {
    val o = new CompilerOptions()
    o.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT5)
    o
  }

  lazy val loader: Loader = if (isProd) {

    lazy val branch: String = if (branchAndHash.branch.isEmpty || branchAndHash.branch == "?") "no-branch" else branchAndHash.branch

    val commitHash = branchAndHash.hash

    lazy val releaseRoot: String = if (commitHash.isEmpty || commitHash == "?") {
      val format = new java.text.SimpleDateFormat("yyyy-MM-dd--HH-mm")
      "dev-" + format.format(new java.util.Date())
    } else {
      commitHash
    }

    val bucketName: String = {
      val publicAssets = "corespring-public-assets"
      if (isProd) {
        val envName = config.getString("ENV_NAME").getOrElse("")
        val bucketCompliantBranchName = branch.replaceAll("feature", "").replaceAll("hotfix", "").replaceAll("/", "-")
        val raw = Seq(publicAssets, envName, bucketCompliantBranchName).filterNot(_.isEmpty).mkString("-").toLowerCase
        val trimmed = raw.take(bucketNameLengthMax)
        StringUtils.substringBeforeLast(trimmed, "-")
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

  def customColors = loader.scripts("custom-colors")("js/corespring/custom-colors")

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

