package org.corespring.v2.player.assets

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ListObjectsRequest
import org.corespring.models.appConfig.Bucket
import play.api.Logger

trait S3PathResolver {
  def resolve(prefix: String, path: String): Seq[String]
}

class DefaultS3PathResolver(s3: AmazonS3, awsConfig: Bucket) extends S3PathResolver {

  private lazy val logger = Logger(this.getClass)

  override def resolve(prefix: String, path: String): Seq[String] = {

    logger.debug(s"function=resolve, prefix=$prefix, path=$path")
    val request = new ListObjectsRequest()
    request.setPrefix(prefix)
    request.setBucketName(awsConfig.bucket)
    val listings = s3.listObjects(request)

    import scala.collection.JavaConversions._
    val out = listings.getObjectSummaries.map { s => s.getKey }.filter { k =>
      k.matches(s"$prefix$path")
    }
    logger.debug(s"function=resolve, prefix=$prefix, path=$path, out=$out")
    out
  }
}
