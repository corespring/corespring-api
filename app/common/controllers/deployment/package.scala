package common.controllers

import com.ee.assets.Loader
import com.ee.assets.deployment.Deployer
import common.views.helpers.Defaults
import controllers.ConcreteS3Service
import play.api.Play
import play.api.Play.current

package object deployment {

  private def isProd: Boolean = Play.isProd

  lazy val loader: Loader = new Loader( if(isProd) Some(s3Deployer) else None, Play.mode, current.configuration)

  lazy val s3Deployer: Deployer = new S3Deployer(ConcreteS3Service.getAmazonClient, bucketName, releaseRoot)

  val bucketName: String = if (isProd) "corespring-public-assets-" + branch else "corespring-dev-tmp-assets"

  lazy val branch : String = if(Defaults.branch.isEmpty || Defaults.branch == "?") "no-branch" else Defaults.branch

  lazy val releaseRoot: String = if (Defaults.commitHashShort.isEmpty || Defaults.commitHashShort == "?") {
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd--HH-mm")
    "dev-" + format.format(new java.util.Date())
  } else {
    Defaults.commitHashShort
  }

}
