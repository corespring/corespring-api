package org.corespring.container.client

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ AmazonS3Exception, ObjectListing }
import grizzled.slf4j.Logger

import scala.collection.JavaConversions._
import scala.util.{ Failure, Success, Try }

class AssetUtils(s3: AmazonS3, bucket: String) {

  private val logger: Logger = Logger(classOf[AssetUtils])

  private def withListing(onException: Throwable => Boolean, notFound: => Boolean)(bucket: String, key: String)(found: ObjectListing => Boolean): Boolean = Try(s3.listObjects(bucket, key)) match {
    case Failure(e) => e match {
      case s3: AmazonS3Exception => if (s3.getStatusCode == 404) {
        logger.trace(s"$bucket/$key - not found")
        notFound
      } else {
        onException(s3)
      }
      case t: Throwable => {
        onException(t)
      }
    }
    case Success(listing) => found(listing)
  }

  private def logExceptionAndReturnFalse(t: Throwable) = {
    if (logger.isDebugEnabled) {
      t.printStackTrace()
    }
    logger.warn(t)
    false
  }

  private def returnTrue(): Boolean = true

  /** If an object isn't found it's ok - nothing happens but it's ok -  return true */
  private val getListing = withListing(logExceptionAndReturnFalse, returnTrue) _

  def copyDir(from: String, to: String): Boolean = {
    logger.debug(s"function=copyDir from=$from to=$to")

    getListing(bucket, from) { listing =>
      listing.getObjectSummaries.foreach { s =>
        val destination = s.getKey.replace(from, to)
        logger.trace(s"function=copyDir, copyObject, from=${s.getKey} to=$destination")
        s3.copyObject(bucket, s.getKey, bucket, destination)
      }
      true
    }
  }

  def deleteDir(path: String): Boolean = {
    logger.debug(s"function=deleteDir from=$path")
    getListing(bucket, path) { listing =>
      val keys = listing.getObjectSummaries.map(_.getKey)
      keys.foreach { k =>
        logger.trace(s"function=deleteDir, delete=$k")
        s3.deleteObject(bucket, k)
      }
      true
    }
  }

}
