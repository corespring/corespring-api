package org.corespring.services.salat

import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.errors.{ GeneralError, PlatformServiceError }
import org.corespring.models.auth.Permission
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.{ services => interface }
import scalaz.{ Failure, Success, Validation }
import scalaz.Scalaz._


class CloneItemService(
  contentCollectionService: interface.ContentCollectionService,
  itemService: interface.item.ItemService,
  orgCollectionService: interface.OrgCollectionService) extends interface.CloneItemService {

  private lazy val logger = Logger(classOf[CloneItemService])

  override def cloneItem(itemId: VersionedId[ObjectId], orgId: ObjectId, targetCollectionId: ObjectId): Validation[PlatformServiceError, VersionedId[ObjectId]] = {

    for {
      item <- itemService.findOneById(itemId).toSuccess(err(s"Can't find item with id: $itemId"))
      itemCollectionId <- if (ObjectId.isValid(item.collectionId)) Success(new ObjectId(item.collectionId)) else Failure(err(s"Item: $itemId has an invalid collection id: ${item.collectionId}"))
      //Note: retrieve multiple permissions in one call - should be lighter on the backend
      perms <- Success(orgCollectionService.getPermissions(orgId, itemCollectionId, targetCollectionId))
      targetPermission <- perms.find(_._1 == targetCollectionId).toSuccess(err(s"Can't find permission for $targetCollectionId"))
      itemCollectionPermission <- perms.find(_._1 == itemCollectionId).toSuccess(err(s"Can't find permission for $itemCollectionId"))
      _ <- canWrite(targetPermission)
      _ <- canClone(itemCollectionPermission)
      clonedItem <- itemService.cloneToCollection(item, targetCollectionId).toSuccess(err(s"Cloning item: $itemId failed"))
    } yield {
      if(itemCollectionId == targetCollectionId) {
        logger.warn(s"the item collectionId (${itemCollectionId}) is the same as the collectionId ($targetCollectionId) - so the cloned Item will be in the same collection")
      }
      clonedItem.id
    }
  }

  type IdPerm = (ObjectId,Option[Permission])

  def err(msg: String) = new GeneralError(msg, None)

  private val canWrite = hasPermission(_: IdPerm, Permission.Write)
  private val canClone = hasPermission(_: IdPerm, Permission.Clone)

  private def hasPermission(idPerm:IdPerm, p: Permission): Validation[PlatformServiceError, Boolean] = {
    val (_, granted) = idPerm
    granted.map(_.has(p)).filter(_ == true).toSuccess(err(""))
  }
}
