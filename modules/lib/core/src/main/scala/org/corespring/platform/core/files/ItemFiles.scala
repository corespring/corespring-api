package org.corespring.platform.core.files

import controllers.CorespringS3Service
import org.corespring.common.log.PackageLogging
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.resource.{Resource, StoredFile}
import scalaz.{Validation, Failure, Success}


case class CloneResourceResult(files: Seq[CloneFileResult])

case class CloneFileResult(file: StoredFile, successful: Boolean)

trait ItemFiles extends PackageLogging {

  def s3service: CorespringS3Service

  def bucket: String

  /** Given a newly versioned item, copy the files on s3 to the new storageKey
    * and upate the file's storage key.
    *
    * @return a Validation
    *         Failure -> a seq of files that were successfully cloned (to allow rollback)
    *         Success -> the updated item
    */
  def cloneStoredFiles(implicit item: Item): Validation[Seq[CloneFileResult], Item] = {

    val resources: Seq[Resource] = item.supportingMaterials ++ item.data

    val result: Seq[CloneResourceResult] = resources.map(cloneResourceFiles)

    val files: Seq[CloneFileResult] = result.map(r => r.files).flatten

    def successful = files.filter(!_.successful).length == 0

    if (successful) Success(item) else Failure(files)
  }

  def cloneResourceFiles(resource: Resource)(implicit item: Item): CloneResourceResult = {
    val result: Seq[CloneFileResult] = resource.files.filter(_.isInstanceOf[StoredFile]).map(f => processFile(resource, f.asInstanceOf[StoredFile]))
    CloneResourceResult(result)
  }

  def processFile(resource: Resource, file: StoredFile)(implicit item: Item): CloneFileResult = try {
    val newStorageKey = StoredFile.storageKey(item.id, resource, file.name)

    if(file.storageKey.isEmpty){
      throw new RuntimeException("this file has no storage key: " + file.name + " id: " + item.id + " resource: " + resource.name)
    }

    Logger.debug("[ItemFiles] clone file: " + file.storageKey + " --> " + newStorageKey)
    s3service.copyFile(bucket, file.storageKey, newStorageKey)
    file.storageKey = newStorageKey
    CloneFileResult(file, true)
  } catch {
    case e : Throwable => {
      Logger.debug("An error occurred cloning the file: " + e.getMessage)
      CloneFileResult(file, false)
    }
  }

}

