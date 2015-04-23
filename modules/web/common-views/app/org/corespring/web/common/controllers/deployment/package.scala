package org.corespring.web.common.controllers

import com.ee.assets.Loader
import com.ee.assets.deployment.Deployer
import org.corespring.assets.CorespringS3ServiceExtended
import play.api.Play
import play.api.Play.current
import org.corespring.web.common.views.helpers.Defaults

package object deployment {

  private def isProd: Boolean = Play.isProd

  lazy val loader: Loader = new Loader(if (isProd) Some(s3Deployer) else None, Play.mode, current.configuration)
  lazy val localLoader: Loader = new Loader(None, Play.mode, current.configuration)

  lazy val s3Deployer: Deployer = new S3Deployer(Some(CorespringS3ServiceExtended.getClient), bucketName, releaseRoot)

  val bucketName: String = {
    val publicAssets = "corespring-public-assets"
    if (isProd) {
      val envName = Defaults.envName("")
      val bucketCompliantBranchName = branch.replaceAll("/", "-")
      Seq(publicAssets, envName, bucketCompliantBranchName).filterNot(_.isEmpty).mkString("-").toLowerCase
    } else {
      "corespring-dev-tmp-assets"
    }
  }

  lazy val branch: String = if (Defaults.branch.isEmpty || Defaults.branch == "?") "no-branch" else Defaults.branch

  lazy val releaseRoot: String = if (Defaults.commitHashShort.isEmpty || Defaults.commitHashShort == "?") {
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd--HH-mm")
    "dev-" + format.format(new java.util.Date())
  } else {
    Defaults.commitHashShort
  }

}
