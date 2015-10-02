package org.corespring.services.errors

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.platform.data.mongo.models.VersionedId

/**
 * The base class for Service errors.
 */
abstract class PlatformServiceError(val message: String, val throwable: Option[Throwable] = None)

case class GeneralError(msg: String, t: Option[Throwable]) extends PlatformServiceError(msg, t)

case class CollectionAuthorizationError(val org: ObjectId, val p: Permission, val collection:ObjectId* )
  extends PlatformServiceError("not authorized")

case class ItemAuthorizationError(val org: ObjectId, val p: Permission, val item:VersionedId[ObjectId]* )
  extends PlatformServiceError("not authorized")

case class ItemNotFoundError(val org: ObjectId, val p: Permission, val item:VersionedId[ObjectId]* )
  extends PlatformServiceError("item not found")

case class ItemUpdateError(val org: ObjectId, val p: Permission, val item:VersionedId[ObjectId]* )
  extends PlatformServiceError("update failed")

case class ItemIdError(val id:VersionedId[ObjectId]* )
  extends PlatformServiceError("id not valid")

case class ObjectIdError(val id:ObjectId* )
  extends PlatformServiceError("id not valid")


object PlatformServiceError {
  def apply(message: String, e: Throwable = null): PlatformServiceError = if (e == null) {
    GeneralError(message, None)
  } else {
    GeneralError(message, Some(e))
  }
}
