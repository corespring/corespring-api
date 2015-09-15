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

object IntegrationSpecification {
  lazy val configuration: Configuration = ???

  lazy val mongoUri = {
    configuration.getString("mongo.db.uri").getOrElse {
      throw new IllegalStateException("no db uri configured")
    }
  }

  lazy val uri = MongoURI(mongoUri)
  lazy val connection: MongoConnection = MongoConnection(uri)
  lazy val db: MongoDB = connection.getDB(uri.database.get)

  private lazy val services = new SalatServices {
    override def db: MongoDB = IntegrationSpecification.this.db

    override def archiveConfig: ArchiveConfig = {
      for {
        c <- configuration.getString("archive.contentCollectionId")
        if (ObjectId.isValid(c))
        o <- configuration.getString("archive.orgId")
        if (ObjectId.isValid(o))
      } yield ArchiveConfig(new ObjectId(c), new ObjectId(o))
    }.getOrElse {
      throw new IllegalStateException("can't init archive")
    }

    override def bucket: Bucket = configuration.getString("AMAZON_ASSETS_BUCKET")
      .map(Bucket(_)).getOrElse {
        throw new IllegalStateException("can't set up the bucket")
      }

    override def s3: AmazonS3 = new AmazonS3Client()

    override def accessTokenConfig: AccessTokenConfig = AccessTokenConfig()

    override implicit def context: Context = new ServicesContext(this.getClass.getClassLoader)
  }
}

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