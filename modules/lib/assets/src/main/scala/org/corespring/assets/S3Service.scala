package org.corespring.assets

import com.amazonaws.services.s3.AmazonS3
import org.corespring.amazon.s3.models.DeleteResponse
import org.corespring.amazon.s3.{ConcreteS3Service, S3Service}
import org.corespring.common.config.AppConfig
import play.api.mvc._


trait S3ServiceClient {
  def s3Service: CorespringS3Service
}

trait CorespringS3Service extends S3Service {
  def copyFile(bucket: String, keyName: String, newKeyName: String)

  def online: Boolean

  def getClient: AmazonS3
}

object EmptyS3Service extends CorespringS3Service {
  def download(bucket: String, fullKey: String, headers: Option[Headers]): SimpleResult = ???

  def delete(bucket: String, keyName: String): DeleteResponse = ???

  def copyFile(bucket: String, keyName: String, newKeyName: String) {}

  override def online: Boolean = ???

  override def upload(bucket: String, keyName: String, predicate: (RequestHeader) => Option[SimpleResult]): BodyParser[Int] = ???

  def getClient = ???
}

class CorespringS3ServiceExtended(key: String, secret: String)
  extends ConcreteS3Service(key: String, secret: String)
  with CorespringS3Service {

  def getClient = client

  def copyFile(bucket: String, keyName: String, newKeyName: String) = client.copyObject(bucket, keyName, bucket, newKeyName)

  def online: Boolean = try {
    client.listBuckets().size() > 0
    true
  } catch {
    case e: Throwable => false
  }
}

object CorespringS3ServiceExtended extends CorespringS3ServiceExtended(AppConfig.amazonKey, AppConfig.amazonSecret)
