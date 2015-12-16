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
      _ <- canOrgWriteTo(orgId, targetCollectionId)
      _ <- canOrgCloneFrom(orgId, itemCollectionId)
      clonedItem <- itemService.cloneToCollection(item, targetCollectionId).toSuccess(err(s"Cloning item: $itemId failed"))
    } yield {
      if(itemCollectionId == targetCollectionId) {
        logger.warn(s"the item collectionId (${itemCollectionId}) is the same as the collectionId ($targetCollectionId) - so the cloned Item will be in the same collection")
      }
      clonedItem.id
    }
  }

  def err(msg: String) = new GeneralError(msg, None)

  private val canOrgWriteTo = hasPermission(_: ObjectId, _: ObjectId, Permission.Write)
  private val canOrgCloneFrom = hasPermission(_: ObjectId, _: ObjectId, Permission.Clone)

  private def hasPermission(orgId: ObjectId, collectionId: ObjectId, p: Permission): Validation[PlatformServiceError, Boolean] = {
    if (orgCollectionService.isAuthorized(orgId, collectionId, p)) {
      Success(true)
    } else {
      Failure(err(s"orgId: $orgId, can't access collectionId: $collectionId, with permission: ${p.name}"))
    }
  }
}
