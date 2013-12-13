package org.corespring.test.utils

import org.corespring.assets.CorespringS3Service
import org.corespring.amazon.s3.models.DeleteResponse
import play.api.mvc._
import play.api.libs.iteratee.{ Done, Input }
import org.corespring.amazon.s3.models.DeleteResponse

package object mocks {

  /**
   * A mock s3 service that will throw an exception if the keyname
   * starts with "bad"
   */
  class MockS3Service extends CorespringS3Service {

    import play.api.mvc.Results._

    def copyFile(bucket: String, keyName: String, newKeyName: String) {
      if (keyName.contains("bad"))
        throw new RuntimeException("bad file")
      else
        Unit
    }

    def upload(bucket: String, keyName: String, predicate: (RequestHeader) => Option[SimpleResult]): BodyParser[Int] = {
      BodyParser { request =>
        Done[Array[Byte], Either[SimpleResult, Int]](Left(Ok), Input.Empty)
      }
    }

    def delete(bucket: String, keyName: String): DeleteResponse = null

    def download(bucket: String, fullKey: String, headers: Option[Headers]): SimpleResult = Ok("")

    def online: Boolean = false

    def getClient = ???
  }

}