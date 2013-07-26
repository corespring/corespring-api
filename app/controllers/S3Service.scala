package controllers

import org.corespring.amazon.s3.models.DeleteResponse
import org.corespring.amazon.s3.{ConcreteS3Service, S3Service}
import play.api.mvc._
import common.config.AppConfig

trait S3ServiceClient {
  def s3Service : CorespringS3Service
}

trait CorespringS3Service extends S3Service {
  def copyFile(bucket: String, keyName: String, newKeyName: String)

  def online:Boolean
}

object EmptyS3Service extends CorespringS3Service{
  def download(bucket: String, fullKey: String, headers: Option[Headers]): Result = ???

  def delete(bucket: String, keyName: String): DeleteResponse = ???

  def copyFile(bucket: String, keyName: String, newKeyName: String) {}

  def online: Boolean = ???

  def upload(bucket: String, keyName: String, predicate: (RequestHeader) => Option[Result]): BodyParser[String] = ???
}

class CorespringS3ServiceImpl(key: String, secret: String)
  extends ConcreteS3Service(key: String, secret: String)(play.libs.Akka.system())
  with CorespringS3Service{

  def copyFile(bucket: String, keyName: String, newKeyName: String) = client.copyObject(bucket, keyName, bucket, newKeyName)

  def online: Boolean = try {
    client.listBuckets().size() > 0
    true
  } catch {
    case e: Throwable => false
  }
}

object CorespringS3ServiceImpl extends CorespringS3ServiceImpl(AppConfig.amazonKey, AppConfig.amazonSecret)
