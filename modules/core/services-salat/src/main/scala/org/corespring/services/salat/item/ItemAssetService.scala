package org.corespring.services.salat.item

import com.amazonaws.services.s3.model.AmazonS3Exception
import grizzled.slf4j.Logger
import org.corespring.models.item.Item
import org.corespring.models.item.resource._
import org.corespring.{ services => interface }
import scalaz.{ Failure, Success, Validation }

class ItemAssetService(copyAsset: (String, String) => Unit, deleteFn: (String) => Unit) extends interface.item.ItemAssetService {

  def tryClone(file: StoredFile, fn: StoredFile => String): CloneFileResult = try {
    val newKey = fn(file)
    CloneFileSuccess(file.copy(storageKey = newKey), newKey)
  } catch {
    case s3Exception: AmazonS3Exception => s3Exception.getStatusCode match {
      case 404 => NotFoundCloneFileFailure(file, s3Exception)
      case _ => CloneFileFailure(file, s3Exception)
    }
    case throwable: Throwable => CloneFileFailure(file, throwable)
  }

  private val logger = Logger(classOf[ItemAssetService])
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
      val r = tryClone(f, { f: StoredFile =>
        val fromKey = StoredFile.storageKey(from.id.id, from.id.version.get, "data", f.name)
        val toKey = StoredFile.storageKey(to.id.id, to.id.version.get, "data", f.name)
        logger.trace(s"function=copyAsset, from=$fromKey, to=$toKey")
        copyAsset(fromKey, toKey)
        toKey
      })
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

    def cloneSupportingMaterialResourceFiles(resource: Resource): CloneResourceResult = {
      val result: Seq[CloneFileResult] = resource.files.filter(_.isInstanceOf[StoredFile]).map(f => processFile(resource, f.asInstanceOf[StoredFile], Some("materials/")))
      CloneResourceResult(result)
    }

    def processFile(resource: Resource, file: StoredFile, resourcePrefix: Option[String] = None): CloneFileResult = tryClone(file, { file =>
      val toKey = StoredFile.storageKey(to.id.id, to.id.version.get, resourcePrefix.getOrElse("") + resource.name, file.name)

      //V1 file key validation
      require(!file.storageKey.isEmpty, s"v1 file ${file.name} has no storageKey")
      require(file.storageKey != file.name, s"v1 file ${file.name} has a storageKey that == the file.name")

      val fromKey = file.storageKey

      if (fromKey != file.storageKey) {
        logger.warn(s"This file has a bad key. id=${to.id}, resource=${resource.name}, file=${file.name}")
      }

      logger.debug("[ItemFiles] clone file: " + fromKey + " --> " + fromKey)
      copyAsset(fromKey, toKey)
      toKey
    })

    val result: Seq[CloneResourceResult] = to.supportingMaterials.map(cloneSupportingMaterialResourceFiles) ++ to.data.map(cloneResourceFiles)
    val v1FileResults: Seq[CloneFileResult] = result.map(r => r.files).flatten
    v1FileResults
  }

  /**
   * Given a newly versioned item, copy the files on s3 to the new storageKey
   * and update the file's storage key.
   *
   * @return a Validation
   *         Failure -> a seq of files that were successfully cloned (to allow rollback)
   *         Success -> the updated item
   */
  override def cloneStoredFiles(from: Item, to: Item): Validation[Seq[CloneFileResult], Item] = {
    logger.trace(s"function=cloneStoredFiles, from=${from.id}, to=${to.id}")
    require(from.id.version.isDefined, s"from item id.version must be defined: ${from.id}")
    require(to.id.version.isDefined, s"to item id.version must be defined: ${from.id}")
    val v1FileResults: Seq[CloneFileResult] = cloneV1Files(from, to)
    val v2FileResults = clonePlayerDefinitionFiles(v1FileResults, from, to)
    val cloneFileResults = v1FileResults ++ v2FileResults

    def wasNotFound(result: CloneFileResult) = result match {
      case NotFoundCloneFileFailure(file, _) => {
        logger.warn(s"File ${file.name} was not found on S3 for ${from.id}")
        true
      }
      case _ => false
    }

    def successful = cloneFileResults.filterNot(result => result.successful || wasNotFound(result)).length == 0
    logger.trace(s"function=cloneStoredFiles, Failed clone result: $cloneFileResults")
    if (successful) Success(to) else Failure(cloneFileResults)
  }

  override def delete(key: String): Unit = deleteFn(key)
}
