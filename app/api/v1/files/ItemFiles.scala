package api.v1.files
import common.log.PackageLogging
import controllers.S3Service
import models.item.Item
import models.item.resource.StoredFile
import models.item.service.ItemServiceClient


trait ItemFiles extends PackageLogging { self : ItemServiceClient =>

  def s3service: S3Service

  def bucket: String

  private def cloneS3File(sourceFile: StoredFile, newId: String): String = {
    Logger.debug("Cloning " + sourceFile.storageKey + " to " + newId)
    val oldStorageKeyIdRemoved = sourceFile.storageKey.replaceAll("^[0-9a-fA-F]+/", "")
    s3service.cloneFile(bucket, sourceFile.storageKey, newId + "/" + oldStorageKeyIdRemoved)
    newId + "/" + oldStorageKeyIdRemoved
  }

  def cloneStoredFiles(oldItem: Item, newItem: Item): Boolean = {
    val newItemId = newItem.id.toString
    try {
      newItem.data.get.files.foreach {
        file => file match {
          case sf: StoredFile =>
            val newKey = cloneS3File(sf, newItemId)
            sf.storageKey = newKey
          case _ =>
        }
      }
      newItem.supportingMaterials.foreach {
        sm =>
          sm.files.filter(_.isInstanceOf[StoredFile]).foreach {
            file =>
              val sf = file.asInstanceOf[StoredFile]
              val newKey = cloneS3File(sf, newItemId)
              sf.storageKey = newKey
          }
      }
      itemService.save(newItem)
      true
    } catch {
      case r: RuntimeException =>
        Logger.error("Error cloning some of the S3 files: " + r.getMessage)
        Logger.error(r.getStackTrace.mkString("\n"))
        false
    }

  }

}

