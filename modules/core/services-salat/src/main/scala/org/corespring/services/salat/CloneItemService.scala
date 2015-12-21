package org.corespring.services.salat

import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.errors.collection.OrgNotAuthorized
import org.corespring.errors.item.{ CloneFailed, ItemNotFound }
import org.corespring.errors.{ GeneralError, PlatformServiceError }
import org.corespring.futureValidation.FutureValidation
import org.corespring.models.auth.Permission
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.salat.bootstrap.SalatServicesExecutionContext
import org.corespring.{ services => interface }
import scala.concurrent.Future
import scalaz.{ Failure, Success, Validation }
import scalaz.Scalaz._

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

  override def cloneItem(itemId: VersionedId[ObjectId], orgId: ObjectId, targetCollectionId: Option[ObjectId] = None): FutureValidation[PlatformServiceError, VersionedId[ObjectId]] = {

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

    for {
      item <- fv(itemService.findOneById(itemId).toSuccess(ItemNotFound(itemId)))
      itemCollectionId <- fv(if (ObjectId.isValid(item.collectionId)) Success(new ObjectId(item.collectionId)) else Failure(err(s"Item: $itemId has an invalid collection id: ${item.collectionId}")))
      targetId <- fv(Success(targetCollectionId.getOrElse(itemCollectionId)))
      //Note: retrieve multiple permissions in one call - should be lighter on the backend
      perms <- FutureValidation(orgCollectionService.getPermissions(orgId, itemCollectionId, targetId).map(r => Success(r)))
      targetPermission <- fv(perms.find(_._1 == targetId).toSuccess(err(s"Can't find permission for $targetId")))
      itemCollectionPermission <- fv(perms.find(_._1 == itemCollectionId).toSuccess(err(s"Can't find permission for $itemCollectionId")))
      _ <- fv(canWrite.tupled(targetPermission))
      _ <- fv(canClone.tupled(itemCollectionPermission))
      clonedItem <- fv(itemService.cloneToCollection(item, targetId).toSuccess(CloneFailed(itemId)))
    } yield {
      if (itemCollectionId == targetId) {
        logger.info(s"the item collectionId (${itemCollectionId}) is the same as the collectionId ($targetCollectionId) - so the cloned Item will be in the same collection")
      }
      clonedItem.id
    }
  }

}
