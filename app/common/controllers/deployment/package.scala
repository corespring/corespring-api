package common.controllers

import com.ee.assets.Loader
import com.ee.assets.deployment.Deployer
import common.utils.string
import common.views.helpers.Defaults
import controllers.ConcreteS3Service
import play.api.Play
import play.api.Play.current

package object deployment {

  private def isProd: Boolean = Play.isProd

  lazy val loader: Loader = if (isProd) new Loader(Some(s3Deployer)) else new Loader()

  lazy val s3Deployer: Deployer = new S3Deployer(ConcreteS3Service.getAmazonClient, bucketName, releaseRoot)

  val bucketName: String = if (isProd) "corespring-public-assets-" + branch else "corespring-dev-tmp-assets"

  lazy val branch : String = if(Defaults.branch.isEmpty || Defaults.branch == "?") "no-branch" else Defaults.branch

  lazy val releaseRoot: String = if (Defaults.commitHashShort.isEmpty || Defaults.commitHashShort == "?") {
    string.pseudoRandomString(9, ('a' to 'z'))
  } else {
    Defaults.commitHashShort
  }

}
