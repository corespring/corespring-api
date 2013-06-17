package common.controllers

import com.ee.assets.Loader
import com.ee.assets.deployment.Deployer
import common.views.helpers.Defaults
import controllers.ConcreteS3Service
import play.api.Play
import play.api.Play.current
import java.util.Date
import common.utils.string

package object deployment {

  private def isProd: Boolean = Play.isProd

  lazy val loader: Loader = if (isProd) new Loader(Some(s3Deployer)) else new Loader()

  lazy val s3Deployer: Deployer = new S3Deployer(ConcreteS3Service.getAmazonClient, bucketName)

  val bucketPrefix: String = if(isProd) "corespring-public-assets" else "corespring-dev-tmp-assets"

  lazy val bucketName : String = {

    def getHash = if(Defaults.commitHashShort.isEmpty || Defaults.commitHashShort == "?") {
      string.pseudoRandomString(9, ('a' to 'z'))
    } else {
      Defaults.commitHashShort
    }

    bucketPrefix + "-" + getHash
  }
}
