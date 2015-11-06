package org.corespring.importing

import java.io.ByteArrayInputStream

import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.transfer.TransferManager
import org.corespring.models.appConfig.Bucket
import org.corespring.models.item.resource.{BaseFile, StoredFile}

import scala.concurrent.Future
import scala.io.Source

private[importing] trait Uploader{
  def upload(filename: String, path: String, file: Source) : Future[StoredFile]
}

class TransferManagerUploader( manager: TransferManager,
                               bucket: Bucket,
                               context : ImportingExecutionContext ) extends Uploader{

  implicit val ec = context.ctx

  def upload(filename: String, path: String, file: Source) = Future {
    val byteArray = file.map(_.toByte).toArray
    val metadata = new ObjectMetadata()
    metadata.setContentLength(byteArray.length)
    val result = manager.upload(bucket.bucket, path, new ByteArrayInputStream(byteArray), metadata).waitForUploadResult
    StoredFile(name = filename, contentType = BaseFile.getContentType(filename), storageKey = result.getKey)
  }
}
