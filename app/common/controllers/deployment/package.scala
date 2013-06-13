package common.controllers

import com.ee.assets.Loader
import com.ee.assets.deployment.Deployer
import common.views.helpers.Defaults
import controllers.ConcreteS3Service
import play.api.Play
import play.api.Play.current

package object deployment {

  private def isProd: Boolean = Play.isProd

  lazy val loader: Loader = if (isProd) new Loader(Some(s3Deployer)) else new Loader()

  lazy val s3Deployer: Deployer = new S3Deployer(ConcreteS3Service.getAmazonClient, publicBucket)

  lazy val publicBucket: String = "corespring-public-assets" + (if (isProd) "-" + Defaults.commitHashShort else "")
}
