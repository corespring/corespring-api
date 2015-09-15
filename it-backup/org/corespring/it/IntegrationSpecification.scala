package org.corespring.it

import akka.util.Timeout
import com.amazonaws.services.s3.{ AmazonS3, AmazonS3Client }
import com.mongodb.casbah.{ MongoConnection, MongoDB, MongoURI }
import com.novus.salat.Context
import org.bson.types.ObjectId
import org.corespring.models.appConfig.{ AccessTokenConfig, ArchiveConfig, Bucket }
import org.corespring.services.salat.ServicesContext
import org.corespring.services.salat.bootstrap.SalatServices
import org.slf4j.LoggerFactory
import org.specs2.specification.{ Fragments, Step }
import play.api.Configuration
import play.api.mvc.Results
import play.api.test.PlaySpecification

import scala.concurrent.duration._

class IntegrationSpecification extends PlaySpecification with Results with ServerSpec {

  sequential

  protected def logger: org.slf4j.Logger = LoggerFactory.getLogger("it.spec.is")

  override def map(fs: => Fragments) = {

    Step(server.start()) ^
      Step(logger.trace("-------------------> server started")) ^
      fs ^
      Step(logger.trace("-------------------> stopping server")) ^
      Step(server.stop)
  }

  override implicit def defaultAwaitTimeout: Timeout = 60.seconds

}