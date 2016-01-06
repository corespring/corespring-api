package org.corespring.assets

import com.amazonaws.services.s3.model.S3Object
import org.corespring.amazon.s3.models.DeleteResponse
import org.corespring.amazon.s3.{ ConcreteS3Service, S3Service }
import play.api.Logger
import play.api.mvc.{ Headers, BodyParser, SimpleResult, RequestHeader }

import scala.concurrent.Future

/**
 * A service that works with 2 key types:
 * - the url encoded key (the preferred key type)
 * - the url decoded key (the deprecated key type)
 *
 * We have both types in s3. At some point we'll want to clean that up
 * but this service will try the default key type and fallback to the
 * fully decoded key type (and log an error if it does).
 * @param underlying
 */
class DoubleKeyCheckS3Service(underlying: ConcreteS3Service) extends S3Service {

  private val logger = Logger(classOf[DoubleKeyCheckS3Service])

  val helper = new EncodingHelper
  /**
   * Checks 2 key types:
   * - a url encoded key first
   * - a url decoded key
   */
  override def download(bucket: String, fullKey: String, headers: Option[Headers]): SimpleResult = {

    val prefix = s"function=download, bucket=$bucket, fullKey=$fullKey"
    logger.info(prefix)

    val encoded = helper.encodedOnce(fullKey)

    logger.trace(s"$prefix - try encoded key: $encoded")
    val result = underlying.download(bucket, encoded, headers)
    val isOk = result.header.status / 100 == 2
    if (isOk) result else {
      logger.error(s"$prefix - the encoded key $encoded can't be found")
      val decoded = helper.decodeCompletely(fullKey)
      logger.trace(s"$prefix - try decoded key: $decoded")
      underlying.download(bucket, helper.decodeCompletely(fullKey))
    }
  }

  override def s3ObjectAndData[A](bucket: String, makeKey: (A) => String)(predicate: (RequestHeader) => Either[SimpleResult, A]): BodyParser[Future[(S3Object, A)]] = {

    val prefix = s"function=s3ObjectAndData, bucket=$bucket"

    logger.info(prefix)

    def mkKey(k: A): String = {
      val key = makeKey(k)
      val encoded = helper.encodedOnce(key)
      logger.debug(s"$prefix - encoded=$encoded")
      encoded
    }

    underlying.s3ObjectAndData(bucket, mkKey)(predicate)
  }

  override def upload(bucket: String, keyName: String, predicate: (RequestHeader) => Option[SimpleResult]): BodyParser[Int] = {
    val prefix = s"function=upload, bucket=$bucket"
    logger.info(prefix)
    val encoded = helper.encodedOnce(keyName)
    logger.debug(s"$prefix - encoded=$encoded")
    underlying.upload(bucket, encoded, predicate)
  }

  override def delete(bucket: String, keyName: String): DeleteResponse = {

    val prefix = s"function=delete, bucket=$bucket, keyName=$keyName"
    logger.info(prefix)
    val encoded = helper.encodedOnce(keyName)
    val response = underlying.delete(bucket, encoded)
    if (response.success) response else {
      logger.error(s"$prefix - failed to delete using encoded: $encoded")
      val decoded = helper.decodeCompletely(keyName)
      logger.debug(s"$prefix - try to delete using decoded key: $decoded")
      underlying.delete(bucket, decoded)
    }
  }
}
