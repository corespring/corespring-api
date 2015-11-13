package org.corespring.it

import java.io.File

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion
import com.amazonaws.services.s3.model.{ DeleteObjectsRequest, ObjectListing }
import com.mongodb.casbah.{ MongoClient, MongoClientURI }
import grizzled.slf4j.Logger
import play.api.test.FakeApplication
import play.api.{ Configuration, Play }

import scala.util.Random

//TODO: Seed field values for ItemApiIntegrationTest?
object ITContext {

  System.setProperty("org.xml.sax.driver", "com.sun.org.apache.xerces.internal.parsers.SAXParser")

  private lazy val logger = Logger(ITContext.getClass)

  private lazy val mongoUri = {
    sys.env.get("IT_MONGO_URI").getOrElse(
      "mongodb://localhost/api-it-tests")
  }

  println(s"[ITContext] > mongoUri=$mongoUri")

  private def dropDb = {
    logger.info(s"drop db: $mongoUri")
    val uri = MongoClientURI(mongoUri)
    val client = MongoClient(uri)
    client.dropDatabase(uri.database.get)
    client.close()
  }

  private lazy val uid = {
    Random.alphanumeric.take(4).mkString.toLowerCase
  }

  private lazy val s3Bucket = {
    val out = sys.env.get("IT_S3_BUCKET").getOrElse(
      s"cs-api-it-tests-$uid")
    logger.info(s"s3 bucket: $out")
    out
  }

  private lazy val conf = {
    val f = new File("conf/application.conf")
    require(f.exists, "conf file doesn't exist?")

    val u = com.typesafe.config.ConfigFactory.parseFile(f)
    val out = new Configuration(u.resolve())
    logger.trace(out.underlying)
    out
  }

  private lazy val s3 = new AmazonS3Client(new AWSCredentials {
    override def getAWSAccessKeyId: String = conf.getString("AMAZON_ACCESS_KEY").get
    override def getAWSSecretKey: String = conf.getString("AMAZON_ACCESS_SECRET").get
  })

  private def cleanBucketIfExists(bucket: String) = {

    def rmListing(l: ObjectListing): Unit = {
      import scala.collection.JavaConversions._
      val keys = l.getObjectSummaries.map { s =>
        new KeyVersion(s.getKey)
      }
      val deleteRequest = new DeleteObjectsRequest(bucket)
      deleteRequest.setKeys(keys)
      s3.deleteObjects(deleteRequest)
      if (l.isTruncated) {
        rmListing(s3.listNextBatchOfObjects(l))
      }
    }

    if (s3.doesBucketExist(s3Bucket)) {
      logger.info(s"rm objects from bucket $s3Bucket")
      val initialListing = s3.listObjects(s3Bucket)
      if (initialListing.getObjectSummaries.size > 0) {
        rmListing(initialListing)
      }
    } else {
      logger.info(s"create bucket $s3Bucket")
      s3.createBucket(s3Bucket)
    }
  }

  def setup = {
    println(s"[ITContext] setup")
    dropDb
    cleanBucketIfExists(s3Bucket)
    PlaySingleton.start(mongoUri, s3Bucket)
  }

  def cleanup = {
    println(s"[ITContext] cleanup...")
    PlaySingleton.stop()
    //cleanBucketIfExists(s3Bucket)
    logger.info(s"delete the bucket $s3Bucket")
    //s3.deleteBucket(s3Bucket)
    println("[ITContext] cleanup complete.")
  }
}

private object PlaySingleton {

  private lazy val logger = Logger(PlaySingleton.getClass)

  def start(mongoUri: String, bucket: String) = Play.maybeApplication.map(_ => Unit).getOrElse {
    logger.info(s"starting app ${new File(".").getAbsolutePath}")

    val config = Map(
      "mongodb.default.uri" -> mongoUri,
      "AMAZON_ASSETS_BUCKET" -> bucket,
      "logger" -> Map("resource" -> "/logback.xml"),
      "api.log-requests" -> false)

    val app: FakeApplication = FakeApplication(
      additionalPlugins = Seq("se.radley.plugin.salat.SalatPlugin"),
      additionalConfiguration = config)

    Play.start(app)
  }

  def stop() = {
    logger.info(s"stopping app ${new File(".").getAbsolutePath}")
    Play.maybeApplication.foreach(_ => Play.stop())
  }

}
