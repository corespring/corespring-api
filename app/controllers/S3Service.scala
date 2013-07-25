package controllers

import play.api.mvc._
import org.corespring.amazon.s3.S3Service
import org.corespring.amazon.s3.models.DeleteResponse

trait S3ServiceClient {
  def s3Service : CorespringS3Service
}

trait CorespringS3Service extends S3Service {
  def cloneFile(bucket: String, keyName: String, newKeyName:String)
  def online:Boolean
  def bucket:String
}

object EmptyS3Service extends CorespringS3Service{
  def download(bucket: String, fullKey: String, headers: Option[Headers]): Result = ???

  def delete(bucket: String, keyName: String): DeleteResponse = ???

  def cloneFile(bucket: String, keyName: String, newKeyName: String) {}

  def online: Boolean = ???

  def bucket: String = ???

  def upload(bucket: String, keyName: String, predicate: (RequestHeader) => Option[Result]): BodyParser[String] = ???
}

/** A temporary s3 service that does nothing while we implement the new S3Service w/ 2.10 + play 2.1.1
 */
//TODO 2.1.1 - Implement an s3Service
