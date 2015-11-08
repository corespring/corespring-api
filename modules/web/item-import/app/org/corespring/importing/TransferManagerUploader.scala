package org.corespring.importing

import java.io.ByteArrayInputStream

import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.transfer.TransferManager
import org.corespring.models.appConfig.Bucket
import org.corespring.models.item.resource.{ BaseFile, StoredFile }
import play.api.Logger

import scala.concurrent.Future
import scala.io.Source

private[importing] trait Uploader {
  def upload(filename: String, path: String, file: Source): Future[StoredFile]
}

class TransferManagerUploader(manager: TransferManager,
  bucket: Bucket,
  context: ImportingExecutionContext) extends Uploader {

  private val logger = Logger(classOf[TransferManagerUploader])

  implicit val ec = context.ctx

  def upload(filename: String, path: String, file: Source) = Future {

    logger.debug(s"function=upload, filename=$filename, path=$path, bucket=$bucket")
    val byteArray = file.map(_.toByte).toArray
    val metadata = new ObjectMetadata()
    metadata.setContentLength(byteArray.length)
    val contentType = BaseFile.getContentType(filename)
    metadata.setContentType(contentType)
    val result = manager.upload(bucket.bucket, path, new ByteArrayInputStream(byteArray), metadata).waitForUploadResult
    StoredFile(name = filename, contentType = contentType, storageKey = result.getKey)
  }
}
