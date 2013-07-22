package controllers

import play.api.mvc._

trait S3ServiceClient {
  def s3Service : S3Service
}

trait S3Service {
  case class S3DeleteResponse(success: Boolean, key: String, msg: String = "")
  def download(bucket: String, fullKey: String, headers: Option[Headers] = None): Result
  def s3upload(bucket: String, keyName: String): BodyParser[Int]
  def s3download(bucket: String, itemId: String, keyName: String): Result
  def delete(bucket: String, keyName: String): S3DeleteResponse
  def cloneFile(bucket: String, keyName: String, newKeyName:String)
  def online:Boolean
  def bucket:String
}

object EmptyS3Service extends S3Service{
  def download(bucket: String, fullKey: String, headers: Option[Headers]): Result = ???

  def s3upload(bucket: String, keyName: String): BodyParser[Int] = ???

  def s3download(bucket: String, itemId: String, keyName: String): Result = ???

  def delete(bucket: String, keyName: String): S3DeleteResponse = ???

  def cloneFile(bucket: String, keyName: String, newKeyName: String) {}

  def online: Boolean = ???

  def bucket: String = ???
  }

/** A temporary s3 service that does nothing while we implement the new S3Service w/ 2.10 + play 2.1.1
 */
//TODO 2.1.1 - Implement an s3Service
