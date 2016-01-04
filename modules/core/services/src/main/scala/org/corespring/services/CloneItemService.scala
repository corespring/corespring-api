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
   * - check that the org that owns the target collectionId has 'clone' permission rights on the collection that owns the item.
   *
   * @param itemId
   * @param collectionId
   * @return
   */
  def cloneItem(
                 itemId: VersionedId[ObjectId],
                 orgId: ObjectId,
                 collectionId: Option[ObjectId] = None): FutureValidation[PlatformServiceError, VersionedId[ObjectId]]
}