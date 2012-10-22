package utils

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.auth.PropertiesCredentials
import tools.nsc.io.File
import play.api.Play
import play.api.Play.current
import web.controllers.utils.ConfigLoader

object S3TestUtil {

  private val s3 = new AmazonS3Client( new PropertiesCredentials( Play.getFile("/conf/AwsCredentials.properties")))

  private val bucket = ConfigLoader.get("AMAZON_ASSETS_BUCKET").get

  def exists( path : String ) : Boolean = {

    println("exists? " + bucket + ", " + path)
    try {
      val metadata = s3.getObjectMetadata(bucket, path)
      println("metadata:")
      println(metadata)
      true
    }
    catch {
      case e : Exception => {
        println("exception occured: " + e.getMessage)
        false
      }
    }
  }
}
