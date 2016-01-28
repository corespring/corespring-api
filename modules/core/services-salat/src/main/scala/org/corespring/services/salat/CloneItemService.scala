package org.corespring.services.salat

import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.errors.collection.OrgNotAuthorized
import org.corespring.errors.item.ItemNotFound
import org.corespring.futureValidation.FutureValidation
import org.corespring.models.auth.Permission
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.salat.bootstrap.SalatServicesExecutionContext
import org.corespring.{ services => interface }

import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

class CloneItemService(
  contentCollectionService: interface.ContentCollectionService,
  itemService: interface.item.ItemService,
  orgCollectionService: interface.OrgCollectionService,
  servicesExecutionContext: SalatServicesExecutionContext)
  extends interface.CloneItemService {

  implicit val ec = servicesExecutionContext.ctx

  private lazy val logger = Logger(classOf[CloneItemService])

  private def fv[E, A](v: Validation[E, A]): FutureValidation[E, A] = FutureValidation(v)

  private def err(s: String) = PlatformServiceError(s)

  override def cloneItem(
    itemId: VersionedId[ObjectId],
    orgId: ObjectId,
    destinationCollectionId: Option[ObjectId] = None): FutureValidation[PlatformServiceError, VersionedId[ObjectId]] = {

    def hasPermission(requestedPermission: Permission,
      e: (ObjectId, Option[Permission], ObjectId) => OrgNotAuthorized)(id: ObjectId, granted: Option[Permission]): Validation[OrgNotAuthorized, Boolean] = {
      granted
        .map(_.has(requestedPermission))
        .filter(_ == true)
        .toSuccess(e(orgId, granted, id))
    }

    import Permission._
    import org.corespring.errors.collection.{ CantCloneFromCollection, CantWriteToCollection }

    val canWrite = hasPermission(Write, CantWriteToCollection _) _
    val canClone = hasPermission(Clone, CantCloneFromCollection _) _

    def getDestinationCollectionId(itemCollectionId: ObjectId) = fv(Success(destinationCollectionId.getOrElse(itemCollectionId)))

    for {
      item <- fv(itemService.findOneById(itemId).toSuccess(ItemNotFound(itemId)))
      itemCollectionId <- fv(if (ObjectId.isValid(item.collectionId)) Success(new ObjectId(item.collectionId)) else Failure(err(s"Item: $itemId has an invalid collection id: ${item.collectionId}")))
      destinationId <- getDestinationCollectionId(itemCollectionId)
      //Note: retrieve multiple permissions in one call - should be lighter on the backend
      allPermissions <- FutureValidation(orgCollectionService.getPermissions(orgId, itemCollectionId, destinationId).map(r => Success(r)))
      destinationPermission <- fv(allPermissions.find(_._1 == destinationId).toSuccess(err(s"Can't find permission for $destinationId")))
      itemCollectionPermission <- fv(allPermissions.find(_._1 == itemCollectionId).toSuccess(err(s"Can't find permission for $itemCollectionId")))
      _ <- fv(canWrite.tupled(destinationPermission))
      _ <- fv(canClone.tupled(itemCollectionPermission))
      clonedItem <- fv(itemService.cloneToCollection(item, destinationId).leftMap(e => err(s"Clone failed: $e")))
    } yield {
      if (itemCollectionId == destinationId) {
        logger.info(s"the item collectionId (${itemCollectionId}) is the same as the collectionId ($destinationCollectionId) - so the cloned Item will be in the same collection")
      }
      clonedItem.id
    }
  }

}
