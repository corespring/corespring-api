package org.corespring.platform.core.files

import org.corespring.assets.CorespringS3Service
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.resource.{ Resource, StoredFile }
import play.api.Logger

import scalaz.{ Failure, Success, Validation }

case class CloneResourceResult(files: Seq[CloneFileResult])

trait CloneFileResult {
  def file: StoredFile
  def successful: Boolean
}

case class CloneFileSuccess(val file: StoredFile, s3Key: String) extends CloneFileResult {
  override def successful: Boolean = true
}

case class CloneFileFailure(val file: StoredFile, err: Throwable) extends CloneFileResult {
  override def successful: Boolean = false
}

trait ItemFiles {

  protected val logger = Logger(classOf[ItemFiles])

  def s3service: CorespringS3Service

  def bucket: String

  /**
   * clone v2 player definition files, if the v1 clone has already tried to copy a file with the same name - skip it.
   * This function is separate from the v1 logic because we don't need to update the storageKey in v2
   * and it makes it clear what the difference is.
   * TODO: V1 tidy up - once we get to clear out v1 this trait can be cleaned up.
   * @return
   */
  protected def clonePlayerDefinitionFiles(alreadyCopied: Seq[CloneFileResult], from: Item, to: Item): Seq[CloneFileResult] = {

    val files = to.playerDefinition.map(_.files).getOrElse(Seq.empty)

    def copyFile(f: StoredFile): Option[CloneFileResult] = if (alreadyCopied.exists(_.file.name == f.name)) {
      None
    } else {
      val r = try {
        val fromKey = StoredFile.storageKey(from.id.id, from.id.version.get, "data", f.name)
        val toKey = StoredFile.storageKey(to.id.id, to.id.version.get, "data", f.name)
        s3service.copyFile(bucket, fromKey, toKey)
        CloneFileSuccess(f, toKey)
      } catch {
        case t: Throwable => {
          CloneFileFailure(f, t)
        }
      }
      Some(r)
    }

    files.flatMap {
      case sf: StoredFile => Some(copyFile(sf))
      case _ => None
    }.flatten
  }

  protected def cloneV1Files(from: Item, to: Item): Seq[CloneFileResult] = {

    def cloneResourceFiles(resource: Resource): CloneResourceResult = {
      val result: Seq[CloneFileResult] = resource.files.filter(_.isInstanceOf[StoredFile]).map(f => processFile(resource, f.asInstanceOf[StoredFile]))
      CloneResourceResult(result)
    }

    def processFile(resource: Resource, file: StoredFile): CloneFileResult = try {
      val toKey = StoredFile.storageKey(to.id.id, to.id.version.get, resource, file.name)

      //V1 file key validation
      require(!file.storageKey.isEmpty, s"v1 file ${file.name} has no storageKey")
      require(file.storageKey != file.name, s"v1 file ${file.name} has a storageKey that == the file.name")

      val fromKey = file.storageKey

      if (fromKey != file.storageKey) {
        logger.warn(s"This file has a bad key. id=${to.id}, resource=${resource.name}, file=${file.name}")
      }

      logger.debug("[ItemFiles] clone file: " + from + " --> " + to)
      println("[ItemFiles] clone file: " + from + " --> " + to)
      s3service.copyFile(bucket, fromKey, toKey)
      file.storageKey = toKey
      CloneFileSuccess(file, toKey)
    } catch {
      case e: Throwable => {
        logger.debug("An error occurred cloning the file: " + e.getMessage)
        CloneFileFailure(file, e)
      }
    }

    val resources: Seq[Resource] = to.supportingMaterials ++ to.data
    val result: Seq[CloneResourceResult] = resources.map(cloneResourceFiles)
    val v1FileResults: Seq[CloneFileResult] = result.map(r => r.files).flatten
    v1FileResults
  }

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
    val v1FileResults: Seq[CloneFileResult] = cloneV1Files(from, to)
    val v2FileResults = clonePlayerDefinitionFiles(v1FileResults, from, to)
    val cloneFileResults = v1FileResults ++ v2FileResults
    def successful = cloneFileResults.filterNot(_.successful).length == 0
    logger.trace(s"Failed clone result: $cloneFileResults")
    if (successful) Success(to) else Failure(cloneFileResults)
  }

}

