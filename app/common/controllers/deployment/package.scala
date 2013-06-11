package common.controllers

import com.ee.assets.Loader
import com.ee.assets.deployment.Deployer
import controllers.ConcreteS3Service

package object deployment {

  lazy val loader : Loader = new Loader(Some(s3Deployer))//if(Play.application.mode == Mode.Prod) new Loader( Some(s3Deployer) ) else new Loader()

  lazy val s3Deployer : Deployer = new S3Deployer(ConcreteS3Service.getAmazonClient, "public-assets")
}
