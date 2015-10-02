package org.corespring.it.assets

import java.io.File

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.transfer.{ Upload, TransferManager }
import org.apache.commons.io.IOUtils
import org.corespring.common.aws.AwsUtil
import org.corespring.common.config.AppConfig
import play.api.Logger

object ImageUtils {

  lazy val logger = Logger(ImageUtils.getClass)
  lazy val client = new AmazonS3Client(credentials)
  lazy val bucket = AppConfig.assetsBucket
  lazy val credentials: AWSCredentials = AwsUtil.credentials()
  lazy val tm: TransferManager = new TransferManager(credentials)

  def upload(file: File, s3Path: String) = {
    require(file.exists)
    logger.debug(s"Uploading image, bucket=$bucket, file=${file.getPath}, key=$s3Path")
    val upload: Upload = tm.upload(bucket, s3Path, file)
    upload.waitForUploadResult()
    s3Path
  }

  def getS3Object(key: String) = client.getObject(bucket, key)

  def imageData(imagePath: String): Array[Byte] = {
    val file = new File(imagePath)
    require(file.exists)
    require(Seq("jpg", "png").exists(s => file.getName.endsWith(s)))
    val bytes = IOUtils.toByteArray(file.toURI)
    bytes
  }

  def exists(path: String): Boolean = {
    try {
      val o = client.getObject(bucket, path)
      println(o.getKey)
      true
    } catch {
      case _: Throwable => false
    }
  }

  def list(path: String): Seq[String] = {
    import scala.collection.JavaConversions._
    val l = client.listObjects(bucket, path)
    l.getObjectSummaries.map { s => s.getKey }
  }

  def delete(path: String) = {
    list(path).foreach { key =>
      println(s"[delete] $key")
      client.deleteObject(bucket, key)
    }
    println(s"[delete] $path")
    client.deleteObject(bucket, path)
  }
}

