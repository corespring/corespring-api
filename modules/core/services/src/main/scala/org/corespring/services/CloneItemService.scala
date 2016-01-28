package org.corespring.services

import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.futureValidation.FutureValidation
import org.corespring.platform.data.mongo.models.VersionedId

import scala.concurrent.Future
import scalaz.Validation

trait CloneItemService {

  /**
   * Clone an item to another collection
   * - check that the orgId has 'clone' permission on the item's collection.
   * - check that the orgId has 'write' permission on the destinationCollectionId.
   *
   * @param itemId
   * @param destinationCollectionId
   * @return
   */
  def cloneItem(
    itemId: VersionedId[ObjectId],
    orgId: ObjectId,
    destinationCollectionId: Option[ObjectId] = None): FutureValidation[PlatformServiceError, VersionedId[ObjectId]]
}
