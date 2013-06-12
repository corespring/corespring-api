package common.controllers

import com.ee.assets.Loader
import com.ee.assets.deployment.Deployer
import common.views.helpers.Defaults
import controllers.ConcreteS3Service
import play.api.{Play, Mode}

package object deployment {

  lazy val loader: Loader = if (isProd) new Loader(Some(s3Deployer)) else new Loader()

  lazy val s3Deployer: Deployer = new S3Deployer(ConcreteS3Service.getAmazonClient, publicBucket)

  private def isProd: Boolean = {
    import play.api.Play.current
    Play.application.mode == Mode.Prod
  }

  lazy val publicBucket: String = "corespring-public-assets" + (if (isProd) "-" + Defaults.commitHashShort else "")
}
