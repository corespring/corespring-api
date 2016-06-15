package org.corespring.services.salat.item

import com.amazonaws.services.s3.model.AmazonS3Exception
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.models.item.Item
import org.corespring.models.item.resource._
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.{ services => interface }
import scalaz.{ Failure, Success, Validation }

class ItemAssetService(copyAsset: (String, String) => Unit, deleteFn: (String) => Unit) extends interface.item.ItemAssetService {

  private val logger = Logger(classOf[ItemAssetService])

  def tryClone(file: StoredFile, fn: StoredFile => String): CloneFileResult = try {
    val newKey = fn(file)
    CloneFileSuccess(file.copy(storageKey = newKey), newKey)
  } catch {
    case s3Exception: AmazonS3Exception => s3Exception.getStatusCode match {
      case 404 => NotFoundCloneFileFailure(file, s3Exception)
      case _ => ErrorThrownCloneFileFailure(file, s3Exception)
    }
    case throwable: Throwable => {
      logger.warn(s"unknown exception thrown: ${throwable.getMessage}")
      ErrorThrownCloneFileFailure(file, throwable)
    }
  }

  /**
   * clone v2 player definition files, if the v1 clone has already tried to copy a file with the same name - skip it.
   * This function is separate from the v1 logic because we don't need to update the storageKey in v2
   * and it makes it clear what the difference is.
   * TODO: V1 tidy up - once we get to clear out v1 this trait can be cleaned up.
   *
   * @return
   */
  protected def clonePlayerDefinitionFiles(alreadyCopied: Seq[CloneFileResult], from: Item, to: Item): Seq[CloneFileResult] = {

    val files = to.playerDefinition.map(_.storedFiles).getOrElse(Seq.empty)

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

    files.flatMap(copyFile)
  }

  def processFile(fromId: VersionedId[ObjectId], toId: VersionedId[ObjectId], resource: Resource, file: StoredFile, resourcePrefix: Option[String] = None, validateStorageKey: Boolean): CloneFileResult = tryClone(file, { file =>
    val toKey = StoredFile.storageKey(toId.id, toId.version.get, resourcePrefix.getOrElse("") + resource.name, file.name)

    if (validateStorageKey) {
      //V1 file key validation
      require(!file.storageKey.isEmpty, s"v1 file ${file.name} has no storageKey")
      require(file.storageKey != file.name, s"v1 file ${file.name} has a storageKey that == the file.name")
    }

    val fromKey = if (file.storageKey == file.name || file.storageKey == null || file.storageKey.isEmpty) {
      StoredFile.storageKey(fromId.id, fromId.version.get, resourcePrefix.getOrElse("") + resource.name, file.name)
    } else file.storageKey

    if (fromKey != file.storageKey) {
      logger.warn(s"This file has a bad key. id=${toId}, resource=${resource.name}, file=${file.name}, fromKey=$fromKey, storageKey=${file.storageKey}")
    }

    logger.debug("[ItemFiles] clone file: " + fromKey + " --> " + fromKey)
    copyAsset(fromKey, toKey)
    toKey
  })

  protected def cloneDataResource(from: Item, to: Item): Option[CloneResourceResult] = {

    def cloneResourceFiles(resource: Resource): CloneResourceResult = {
      val result: Seq[CloneFileResult] = resource.storedFiles.map(processFile(from.id, to.id, resource, _, validateStorageKey = true))
      CloneResourceResult(result)
    }

    to.data.map(cloneResourceFiles)
  }

  private def cloneSupportingMaterialFiles(from: Item, to: Item): Seq[CloneResourceResult] = {

    def cloneSupportingMaterialResourceFiles(resource: Resource): CloneResourceResult = {
      val result: Seq[CloneFileResult] = resource.storedFiles.map(processFile(from.id, to.id, resource, _, Some("materials/"), validateStorageKey = false))
      CloneResourceResult(result)
    }

    to.supportingMaterials.map(cloneSupportingMaterialResourceFiles)
  }

  /**
   * Given a newly versioned item, copy the files on s3 to the new storageKey
   * and update the file's storage key.
   *
   * @return a Validation
   *         Failure -> a seq of files that were successfully cloned (to allow rollback)
   *         Success -> the updated item
   */
  override def cloneStoredFiles(from: Item, to: Item): Validation[CloneError, Item] = {
    logger.trace(s"function=cloneStoredFiles, from=${from.id}, to=${to.id}")

    if (from.id.version.isEmpty) {
      Failure(MissingVersionFromId(from.id))
    } else if (to.id.version.isEmpty) {
      Failure(MissingVersionFromId(to.id))
    } else {
      val v1DataResult = (Seq.empty ++ cloneDataResource(from, to)).map(_.results).flatten
      val supportingMaterialResults = cloneSupportingMaterialFiles(from, to).map(_.results).flatten
      val v2FileResults = clonePlayerDefinitionFiles(v1DataResult, from, to)

      val cloneFileResults = v1DataResult ++ supportingMaterialResults ++ v2FileResults

      /**
       * Note: we have decided to not return a Failure if a file isn't found on clone.
       * We may want to review this at some point.
       */
      val grouped = cloneFileResults.groupBy {
        case nf: NotFoundCloneFileFailure => "not-found"
        case f: CloneFileFailure => "failures"
        case _ => "ok"
      }

      val failures = grouped.get("failures").getOrElse(Nil)

      failures.foreach {
        case ErrorThrownCloneFileFailure(file, err) => {
          logger.error(s"function=cloneItem, file=$file, err=${err.getMessage} - CloneFileFailure")
          if (logger.isWarnEnabled) {
            err.printStackTrace()
          }
        }
        case _ => Unit
      }

      grouped.get("not-found").getOrElse(Nil).foreach {
        case ErrorThrownCloneFileFailure(file, err) => {
          logger.error(s"function=cloneItem, file=$file, err=${err.getMessage} - CloneFileFailure")
          if (logger.isWarnEnabled) {
            err.printStackTrace()
          }
        }
        case _ => Unit
      }

      logger.trace(s"function=cloneStoredFiles, Failed clone result: $cloneFileResults")
      if (failures.length == 0) Success(to) else Failure(CloningFailed(cloneFileResults))
    }
  }

  override def delete(key: String): Unit = deleteFn(key)
}
