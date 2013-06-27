package utils

import controllers.S3Service
import play.api.mvc.{Headers, Result, BodyParser}


package object mocks {


  /** A mock s3 service that will throw an exception if the keyname
    * starts with "bad"
    */
  class MockS3Service extends S3Service {

    import play.api.mvc.Results._

    def cloneFile(bucket: String, keyName: String, newKeyName: String) {
      if (keyName.contains("bad"))
        throw new RuntimeException("bad file")
      else
        Unit
    }

    def s3upload(bucket: String, keyName: String): BodyParser[Int] = null

    def s3download(bucket: String, itemId: String, keyName: String): Result = Ok("")

    def bucket: String = null

    def delete(bucket: String, keyName: String): this.type#S3DeleteResponse = null

    def download(bucket: String, fullKey: String, headers: Option[Headers]): Result = Ok("")

    def online: Boolean = false
  }

}