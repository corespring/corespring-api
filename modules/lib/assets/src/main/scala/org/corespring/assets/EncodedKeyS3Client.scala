package org.corespring.assets

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model._
import org.corespring.common.url.EncodingHelper
import play.api.Logger

/**
 * By default uses the encoded key for get/put/delete/copy, and falls back to the decoded key.
 *
 * Note: It would be preferable to have our own asset service and put s3 behind that,
 * but that would require some re-plumbing.
 *
 * @param credentials
 */
class EncodedKeyS3Client(credentials: AWSCredentials) extends AmazonS3Client(credentials) {

  private val logger = Logger(classOf[EncodedKeyS3Client])

  val helper = new EncodingHelper()

  override def listObjects(r: ListObjectsRequest): ObjectListing = {

    val originalPrefix = r.getPrefix
    val isUnencoded = helper.isUnencoded(originalPrefix)

    logger.info(s"function=listObjects, prefix=${r.getPrefix}")
    r.setPrefix(helper.encodedOnce(r.getPrefix))

    logger.debug(s"function=listObjects, encoded prefix=${r.getPrefix}")
    val encodedListing = super.listObjects(r)

    if (encodedListing.getObjectSummaries.size == 0) {
      r.setPrefix(helper.decodeCompletely(r.getPrefix))
      logger.debug(s"function=listObjects, decoded prefix=${r.getPrefix}")
      val rawListing = super.listObjects(r)
      rawListing
    } else {

      if (isUnencoded) {

        logger.debug(s"the prefix is unencoded, decode the prefix")
        import scala.collection.JavaConversions._
        encodedListing.getObjectSummaries.foreach { s =>
          val encodedPrefix = helper.encodedOnce(originalPrefix)
          val newKey = s.getKey.replace(encodedPrefix, originalPrefix)
          s.setKey(newKey)
        }
      }
      encodedListing
    }
  }

  override def copyObject(r: CopyObjectRequest): CopyObjectResult = {

    logger.info(s"function=copyObject, sourceKey=${r.getSourceKey}, destinationKey=${r.getDestinationKey}")
    r.setDestinationKey(helper.encodedOnce(r.getDestinationKey))

    try {
      r.setSourceKey(helper.encodedOnce(r.getSourceKey))
      logger.info(s"function=copyObject, encoded: sourceKey=${r.getSourceKey}, destinationKey=${r.getDestinationKey}")
      super.copyObject(r)
    } catch {
      case t: Throwable => {

        logger.warn(s"function=copyObject, error for sourceKey=${r.getSourceKey}, destinationKey=${r.getDestinationKey}")
        if (logger.isDebugEnabled) {
          logger.debug(s"function=copyObject, error for sourceKey=${r.getSourceKey}, destinationKey=${r.getDestinationKey} - stacktrace")
          t.printStackTrace()
        }

        r.setSourceKey(helper.decodeCompletely(r.getSourceKey))
        logger.debug(s"function=copyObject, decoded source: sourceKey=${r.getSourceKey}, destinationKey=${r.getDestinationKey}")
        super.copyObject(r)
      }
    }
  }

  override def putObject(r: PutObjectRequest): PutObjectResult = {
    logger.info(s"function=putObject, key=${r.getKey}")
    r.setKey(helper.encodedOnce(r.getKey))
    logger.info(s"function=putObject, encodedKey=${r.getKey}")
    super.putObject(r)
  }

  override def deleteObject(r: DeleteObjectRequest): Unit = {
    logger.info(s"function=deleteObject, key=${r.getKey}")
    try {
      r.setKey(helper.encodedOnce(r.getKey))
      logger.debug(s"function=deleteObject, encodedKey=${r.getKey}")
      super.deleteObject(r)
    } catch {
      case t: Throwable => {

        logger.warn(s"function=deleteObject, error for key=${r.getKey}, message: ${t.getMessage}")

        if (logger.isDebugEnabled) {
          logger.debug(s"function=deleteObject, stacktrace for key=${r.getKey}")
          t.printStackTrace()
        }

        r.setKey(helper.decodeCompletely(r.getKey))
        logger.debug(s"function=deleteObject, decodedKey=${r.getKey}")
        super.deleteObject(r)
      }
    }
  }

  override def getObjectMetadata(r: GetObjectMetadataRequest): ObjectMetadata = {
    logger.info(s"function=getObjectMetadata, r=$r")
    try {
      r.setKey(helper.encodedOnce(r.getKey))
      logger.debug(s"function=getObjectMetadata, encodedKey=${r.getKey}")
      super.getObjectMetadata(r)
    } catch {
      case t: Throwable => {
        logger.warn(s"error for key=${r.getKey}, message=${t.getMessage}")

        if (logger.isTraceEnabled) {
          logger.warn(s"stacktrace for key=${r.getKey}")
          t.printStackTrace()
        }

        r.setKey(helper.decodeCompletely(r.getKey))
        logger.debug(s"function=getObjectMetadata, decodedKey=${r.getKey}")
        super.getObjectMetadata(r)
      }
    }
  }

  override def getObject(r: GetObjectRequest): S3Object = {
    logger.info(s"function=getObject, r=$r")
    try {
      r.setKey(helper.encodedOnce(r.getKey))
      logger.debug(s"function=getObject, encodedKey=${r.getKey}")
      super.getObject(r)
    } catch {
      case t: Throwable => {
        logger.warn(s"error for key=${r.getKey}, message=${t.getMessage}")

        if (logger.isTraceEnabled) {
          logger.warn(s"stacktrace for key=${r.getKey}")
          t.printStackTrace()
        }

        r.setKey(helper.decodeCompletely(r.getKey))
        logger.debug(s"function=getObject, decodedKey=${r.getKey}")
        super.getObject(r)
      }
    }
  }

}