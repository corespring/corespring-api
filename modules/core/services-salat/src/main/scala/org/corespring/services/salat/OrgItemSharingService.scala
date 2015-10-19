package org.corespring.services.salat

import com.mongodb.casbah.Imports._
import org.corespring.models.auth.Permission
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.errors.ItemAuthorizationError
import org.corespring.services.item.ItemService

import scalaz.{ Failure, Success }

class OrgItemSharingService(itemService: ItemService) extends org.corespring.services.OrgItemSharingService {

  /**
   * Unshare the specified items from the specified collections
   */
  override def unShareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collIds: Seq[ObjectId]): Validation[PlatformServiceError, Seq[VersionedId[ObjectId]]] = {
    for {
      canUpdateAllCollections <- isAuthorized(orgId, collIds, Permission.Write)
      successfullyRemovedItems <- itemService.removeCollectionIdsFromShared(items, collIds)
    } yield successfullyRemovedItems
  }
  /**
   * Share items to the collection specified.
   * - must ensure that the context org has write access to the collection
   * - must ensure that the context org has read access to the items being added
   * TODO: Do we check perms here? or keep it outside of this scope?
   * We'll have to filter the individual item ids anyway
   */
  override def shareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collId: ObjectId): Validation[PlatformServiceError, Seq[VersionedId[ObjectId]]] = {

    def allowedToWriteCollection = {
      isAuthorized(orgId, collId, Permission.Write)
    }

    def allowedToReadItems = {
      val objectIds = items.map(i => i.id)
      // get a list of any items that were not authorized to be added
      itemService.findMultipleById(objectIds: _*).filterNot(item => {
        // get the collections to test auth on (owner collection for item, and shared-in collections)
        val collectionsToAuth = new ObjectId(item.collectionId) +: item.sharedInCollections
        // does org have read access to any of these collections
        val collectionsAuthorized = collectionsToAuth.
          filter(collectionId => isAuthorized(orgId, collectionId, Permission.Read).isSuccess)
        collectionsAuthorized.nonEmpty
      }) match {
        case Stream.Empty => Success()
        case notAuthorizedItems => {
          logger.error(s"[allowedToReadItems] unable to read items: ${notAuthorizedItems.map(_.id)}")
          Failure(ItemAuthorizationError(orgId, Permission.Read, notAuthorizedItems.map(_.id): _*))
        }
      }
    }

    def saveUnsavedItems = {
      itemService.addCollectionIdToSharedCollections(items, collId)
    }

    for {
      canWriteToCollection <- allowedToWriteCollection
      canReadAllItems <- allowedToReadItems
      sharedItems <- saveUnsavedItems
    } yield sharedItems
  }

}
