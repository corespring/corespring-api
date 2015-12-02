package filters

import java.io.{ InputStream, PipedInputStream, PipedOutputStream }

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ ObjectMetadata, S3Object }
import org.apache.commons.io.IOUtils
import play.api.Logger
import play.api.http.HeaderNames._
import play.api.libs.iteratee.Iteratee
import play.api.mvc.Results._
import play.api.mvc.{ Filter, RequestHeader, SimpleResult }

import scala.concurrent.{ ExecutionContext, Future }

trait CacheFilter extends Filter {

  type Req2Res = RequestHeader => Future[SimpleResult]

  def intercept(path: String): Boolean

  def s3: AmazonS3

  def appVersion: String

  def s3CacheDir: String = "cache-filter"

  def bucket: String

  def gzipEnabled: Boolean

  def futureQueue: FutureQueuer

  lazy val logger = Logger(classOf[CacheFilter])

  implicit def ec: ExecutionContext

  private def acceptsGzip(implicit rh: RequestHeader): Boolean = {
    rh.headers.get(ACCEPT_ENCODING).map(_.split(',').exists(_.trim == "gzip")).getOrElse(false)
  }

  private def s3Key(rh: RequestHeader) = {
    val base = s"$s3CacheDir/$appVersion/${rh.path}"
    if (gzipEnabled && acceptsGzip(rh)) {
      s"$base.gz"
    } else {
      base
    }
  }.replace("//", "/")

  private def getHeader(rh: SimpleResult, key: String): String = {
    val headers = rh.header.headers
    headers.get(key).getOrElse {
      throw new RuntimeException(s"Response is missing $key header. This must be supplied.")
    }
  }

  private def loadFromS3(underlyingCall: Req2Res)(implicit rh: RequestHeader): Future[SimpleResult] = {

    val key = s3Key(rh)

    futureQueue.queued(key) {

      logger.info(s"function=loadFromS3, key=$key")

      def etagMatches(key: String, etag: String): Future[Boolean] = Future {
        try {
          val metadataResult = s3.getObjectMetadata(bucket, key)
          val matches = metadataResult.getETag == etag
          logger.trace(s"function=etagMatches, matches?${matches}")
          matches
        } catch {
          case t: Throwable => false
        }
      }

      def invokeUnderlyingAndPutOnS3(key: String): Future[SimpleResult] = {

        logger.warn(s"function=invokeUnderlyingAndPutOnS3, key=$key, id=${rh.id} - about to call the underlying (and expensive) operation")
        val futureAssetResult = underlyingCall(rh)

        futureAssetResult.flatMap { res =>

          val outputStream = new PipedOutputStream()
          val inputStream = new PipedInputStream(outputStream)

          val iteratee = Iteratee.foreach[Array[Byte]] { bytes =>
            logger.trace(s"invokeUnderlyingAndPutOnS3 - iteratee - write bytes...")
            outputStream.write(bytes)
          }

          val metadata = new ObjectMetadata()

          metadata.setContentLength(getHeader(res, CONTENT_LENGTH).toLong)
          metadata.setContentType(getHeader(res, CONTENT_TYPE))

          logger.trace(s"function=invokeUnderlyingAndPutOnS3, headers=${res.header.headers}")

          res.header.headers.get(CONTENT_ENCODING).map { e =>
            metadata.setContentEncoding(e)
          }

          logger.debug(s"function=invokeUnderlyingAndPutOnS3, key=$key, id=${rh.id}, put response on s3")
          // Feed the body into the iteratee
          val f: Future[Unit] = (res.body |>>> iteratee)
          val putResult = s3.putObject(bucket, key, inputStream, metadata)
          val o: Future[SimpleResult] = f.andThen {
            case result =>
              logger.trace(s"function=invokeUnderlyingAndPutOnS3, result=$result")
              logger.debug(s"function=invokeUnderlyingAndPutOnS3, id=${rh.id} close the output and input streams")
              // Close the output stream whether there was an error or not
              outputStream.close()
              inputStream.close()
              // Get the result or rethrow the error
              result.get
          }.map(_ => res.withHeaders(ETAG -> putResult.getETag))
          o
        }
      }

      def loadS3Object(key: String): Future[Option[S3Object]] = Future {
        try {
          Some(s3.getObject(bucket, key))
        } catch {
          case t: Throwable => None
        }
      }

      def s3oToResult(s3o: S3Object): Future[SimpleResult] = Future {

        logger.info(s"function=s3oToResult, s3o.getKey=${s3o.getKey}")
        val inputStream: InputStream = s3o.getObjectContent()
        val metadata = s3o.getObjectMetadata
        val contentType = metadata.getContentType()
        val encoding = {
          val e = metadata.getContentEncoding
          if (e == null) None else Some(e)
        }

        val headers = Seq(
          CONTENT_TYPE -> (if (contentType != null) contentType else "application/octet-stream"),
          CONTENT_LENGTH.toString -> metadata.getContentLength.toString,
          ETAG -> metadata.getETag) ++ encoding.map(e => CONTENT_ENCODING -> e)

        logger.debug(s"function=s3ToResult, headers=$headers")
        /**
         * Note: We can't just return a SimpleResult with a stream from s3,
         * because there may be multiple requests waiting for this.
         * Instead we need to read the stream into a byte array and return that.
         */

        val bytes = IOUtils.toByteArray(inputStream)
        IOUtils.closeQuietly(inputStream)
        val result = Ok(bytes).withHeaders(headers: _*)
        logger.debug(s"function=s3ToResult, result=$result")
        result
      }

      rh.headers.get(IF_NONE_MATCH).map { etag =>
        etagMatches(key, etag).flatMap { matches =>
          if (matches) Future(NotModified) else invokeUnderlyingAndPutOnS3(key)
        }
      }.getOrElse {
        loadS3Object(key).flatMap { s3o =>
          s3o.map { o => s3oToResult(o) }.getOrElse(invokeUnderlyingAndPutOnS3(key))
        }
      }
    }
  }

  override def apply(f: Req2Res)(rh: RequestHeader): Future[SimpleResult] = {
    if (intercept(rh.path)) {
      loadFromS3(f)(rh)
    } else {
      f(rh)
    }
  }
}
