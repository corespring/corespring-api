package org.corespring.api.v1

import org.corespring.models.item.resource.{ StoredFile, BaseFile, Resource }
import org.corespring.models.item.{ TaskInfo, Item }

import scalaz.{ Failure, Success, Validation }

class ItemApiItemValidation {

  def validateItem(dbItem: Item, item: Item): Validation[String, Item] = {
    try {
      Success(item.copy(
        id = dbItem.id,
        collectionId = if (item.collectionId.isEmpty) dbItem.collectionId else item.collectionId,
        taskInfo = item.taskInfo.map(_.copy(extended = dbItem.taskInfo.getOrElse(TaskInfo()).extended)),
        data = addDataStorageKeys(dbItem, item),
        supportingMaterials = addSupportingMaterialsStorageKeys(dbItem, item)))
    } catch {
      case e: RuntimeException =>
        Failure(s"Validation error: ${e.getMessage()}")
    }
  }

  private def addDataStorageKeys(dbItem: Item, item: Item): Option[Resource] = {
    item.data.map(resource => addStorageKeys(dbItem.data, resource))
  }

  private def addSupportingMaterialsStorageKeys(dbItem: Item, item: Item): Seq[Resource] = {
    val res = item.supportingMaterials.map(sm => dbItem.supportingMaterials.find(_.name == sm.name) match {
      case Some(dbItemResource) => addStorageKeys(Some(dbItemResource), sm)
      case _ => throw new RuntimeException(s"Resource ${sm.name} not found in dbItem.supportingMaterials")
    })
    res
  }

  private def addStorageKeys(dbItemResource: Option[Resource], itemResource: Resource) = {

    def addStorageKeys(dbItemFiles: Seq[BaseFile], itemFiles: Seq[BaseFile]) = {
      def findDbItemFile(name: String) = {
        dbItemFiles
          .map({
            case x: StoredFile => Some(x)
            case _ => None
          })
          .filter(_.isDefined)
          .map(_.get)
          .find(_.name == name)
      }
      itemFiles.map({
        case storedFile: StoredFile => findDbItemFile(storedFile.name) match {
          case Some(dbItemFile) => storedFile.copy(storageKey = dbItemFile.storageKey)
          case _ => throw new RuntimeException(s"StoredFile ${storedFile.name} not found in dbItem.data")
        }
        case otherFile => otherFile
      })
    }

    if (!dbItemResource.isDefined) {
      throw new RuntimeException("dbItem.data is not defined")
    }
    itemResource.copy(
      files = addStorageKeys(dbItemResource.get.files, itemResource.files))
  }
}
