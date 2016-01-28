package org.corespring.errors

import com.mongodb.casbah.Imports._
import org.corespring.models.ContentCollection
import org.corespring.models.auth.Permission
import org.corespring.platform.data.mongo.models.VersionedId

/**
 * The base class for Service errors.
 */
abstract class PlatformServiceError(val message: String, val throwable: Option[Throwable] = None)

case class GeneralError(msg: String, t: Option[Throwable]) extends PlatformServiceError(msg, t)

object collection {
  case class OrgNotAuthorized(val action: String, val org: ObjectId, val requestedPermission: Permission, val grantedPermission: Option[Permission], val collectionId: ObjectId)
    extends PlatformServiceError(
      s"""[$action] - Org $org not authorized for collection $collectionId with permission ${requestedPermission.name}.${grantedPermission.map(p => s" Need ${p.name}.").getOrElse("")}""")

  def CantWriteToCollection(org: ObjectId, granted: Option[Permission], collectionId: ObjectId) = {
    OrgNotAuthorized("write to collection", org, Permission.Write, granted, collectionId)
  }

  def CantCloneFromCollection(org: ObjectId, granted: Option[Permission], collectionId: ObjectId) = {
    OrgNotAuthorized("clone from collection", org, Permission.Clone, granted, collectionId)
  }

}

case class CollectionInsertError(val collection: ContentCollection, t: Option[Throwable])
  extends PlatformServiceError(s"Error inserting collection ${collection}.", t)

case class ItemAuthorizationError(val org: ObjectId, val p: Permission, val items: VersionedId[ObjectId]*)
  extends PlatformServiceError(s"Org $org cannot access item(s) $items with permission $p.")

object item {
  case class OrgNotAuthorized(val org: ObjectId, val requestedPermission: Permission, val itemId: VersionedId[ObjectId])
    extends PlatformServiceError(
      s"Org $org not authorized to access item: $itemId with permission $requestedPermission.")

  case class ItemNotFound(val id: VersionedId[ObjectId])
    extends PlatformServiceError(s"Can't find item with id: $id")

  case class CloneFailed(val id: VersionedId[ObjectId])
    extends PlatformServiceError(s"Clone failed for item: $id")
}

case class ItemUpdateError(val org: ObjectId, val p: Permission, val item: VersionedId[ObjectId]*)
  extends PlatformServiceError(s"Org $org cannot update item(s) ${item} with permission $p.")

case class ItemShareError(val items: Seq[VersionedId[ObjectId]], collection: ObjectId)
  extends PlatformServiceError(s"Error adding item(s) $items to collection $collection.")

case class ItemUnShareError(val failedItems: Seq[VersionedId[ObjectId]], collection: Seq[ObjectId])
  extends PlatformServiceError(s"Error removing item(s) $failedItems from collection $collection.")

case class ItemIdError(val id: VersionedId[ObjectId]*)
  extends PlatformServiceError(s"Id not valid: $id")

case class ObjectIdError(val id: ObjectId*)
  extends PlatformServiceError(s"ObjectId not valid: $id")

case class OrganizationAddCollectionError(val org: ObjectId, val collId: ObjectId, val p: Permission, t: Option[Throwable])
  extends PlatformServiceError(s"Error adding collection $collId to org $org with permission $p.")

object PlatformServiceError {
  def apply(message: String, e: Throwable = null): PlatformServiceError = if (e == null) {
    GeneralError(message, None)
  } else {
    GeneralError(message, Some(e))
  }

}
