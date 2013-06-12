package common.controllers

import com.ee.assets.Loader
import com.ee.assets.deployment.Deployer
import controllers.ConcreteS3Service
import common.views.helpers.Defaults
import play.api.{Play, Mode}
import play.api.Play.current

package object deployment {

  lazy val loader : Loader = new Loader(Some(s3Deployer))//if(Play.application.mode == Mode.Prod) new Loader( Some(s3Deployer) ) else new Loader()

  lazy val s3Deployer : Deployer = new S3Deployer(ConcreteS3Service.getAmazonClient, publicBucket)

  private def isProd : Boolean = Play.application.mode == Mode.Prod

  lazy val publicBucket : String = "corespring-public-assets" + (if(isProd) "-" + Defaults.commitHashShort else "")
}
