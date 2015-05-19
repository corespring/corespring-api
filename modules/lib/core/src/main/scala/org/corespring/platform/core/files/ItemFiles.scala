package org.corespring.platform.core.files

import org.corespring.assets.CorespringS3Service
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.resource.{ Resource, StoredFile }
import play.api.Logger
import scalaz.{ Validation, Failure, Success }

case class CloneResourceResult(files: Seq[CloneFileResult])

case class CloneFileResult(file: StoredFile, throwable: Option[Throwable]) {
  def successful = throwable.isEmpty
}

trait ItemFiles {

  protected val logger = Logger(classOf[ItemFiles])

  def s3service: CorespringS3Service

  def bucket: String

  /**
   * Given a newly versioned item, copy the files on s3 to the new storageKey
   * and upate the file's storage key.
   *
   * @return a Validation
   *         Failure -> a seq of files that were successfully cloned (to allow rollback)
   *         Success -> the updated item
   */
  protected def cloneStoredFiles(from: Item, to: Item): Validation[Seq[CloneFileResult], Item] = {

    require(from.id.version.isDefined, s"from item id.version must be defined: ${from.id}")
    require(to.id.version.isDefined, s"to item id.version must be defined: ${from.id}")

    def cloneResourceFiles(resource: Resource): CloneResourceResult = {
      val result: Seq[CloneFileResult] = resource.files.filter(_.isInstanceOf[StoredFile]).map(f => processFile(resource, f.asInstanceOf[StoredFile]))
      CloneResourceResult(result)
    }

    def processFile(resource: Resource, file: StoredFile): CloneFileResult = try {
      val toKey = StoredFile.storageKey(to.id.id, to.id.version.get, resource, file.name)

      val fromKey = if (file.storageKey == file.name || file.storageKey.isEmpty) {
        StoredFile.storageKey(from.id.id, from.id.version.get, resource, file.name)
      } else {
        file.storageKey
      }

      if (fromKey != file.storageKey) {
        logger.warn(s"This file has a bad key. id=${to.id}, resource=${resource.name}, file=${file.name}")
      }

      logger.debug("[ItemFiles] clone file: " + from + " --> " + to)
      s3service.copyFile(bucket, fromKey, toKey)
      file.storageKey = toKey
      CloneFileResult(file, None)
    } catch {
      case e: Throwable => {
        logger.debug("An error occurred cloning the file: " + e.getMessage)
        CloneFileResult(file, Some(e))
      }
    }

    val resources: Seq[Resource] = to.supportingMaterials ++ to.data
    val result: Seq[CloneResourceResult] = resources.map(cloneResourceFiles)
    val files: Seq[CloneFileResult] = result.map(r => r.files).flatten

    def successful = files.filterNot(_.successful).length == 0

    logger.trace(s"Failed clone result: $files")

    if (successful) Success(to) else Failure(files)
  }

}

