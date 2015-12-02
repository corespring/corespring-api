package org.corespring.services

import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.platform.data.mongo.models.VersionedId

import scalaz.Validation

/**
 * The sharing logic here and the access granting logic in OrgCollectionService,
 * could be abstracted into a SharingService[WHAT,WHO].
 */
trait ShareItemWithCollectionsService {

  def unShareAllItemsFromCollection(collectionId: ObjectId): Validation[PlatformServiceError, Unit]
  /**
   * Share items to the collection specified.
   * - must ensure that the context org has write access to the collection
   * - must ensure that the context org has read access to the items being added
   *
   * @param orgId
   * @param items
   * @param collId
   * @return
   */
  def shareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collId: ObjectId): Validation[PlatformServiceError, Seq[VersionedId[ObjectId]]]

  /**
   * Unshare the specified items from the specified collections
   *
   * @param orgId
   * @param items - sequence of items to be unshared from
   * @param collIds - sequence of collections to have the items removed from
   * @return
   */
  def unShareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collIds: ObjectId*): Validation[PlatformServiceError, Seq[VersionedId[ObjectId]]]

  /**
   * Is the item shared by the collection
   * @param itemId
   * @param collId
   * @return
   */
  def isItemSharedWith(itemId: VersionedId[ObjectId], collId: ObjectId): Boolean
}
