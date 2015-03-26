package org.corespring.v2.auth.wired

import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.{ ItemAccess, ItemAuth }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory

import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

trait ItemAuthWired extends ItemAuth[OrgAndOpts] {

  lazy val logger = V2LoggerFactory.getLogger("auth.ItemAuthWired")

  def itemService: ItemService

  def itemTransformer: ItemTransformer

  def access: ItemAccess

  override def loadForRead(itemId: String)(implicit identity: OrgAndOpts): Validation[V2Error, Item] = canWithPermission(itemId, Permission.Read)

  override def loadForWrite(itemId: String)(implicit identity: OrgAndOpts): Validation[V2Error, Item] = canWithPermission(itemId, Permission.Write)

  override def save(item: Item, createNewVersion: Boolean)(implicit identity: OrgAndOpts): Unit = {
    loadForWrite(item.id.toString) match {
      case Success(dbItem) => itemService.save(item, createNewVersion)
      case Failure(msg) => throw new RuntimeException(s"Error saving $msg")
    }
  }

  override def insert(item: Item)(implicit identity: OrgAndOpts): Option[VersionedId[ObjectId]] = {
    for {
      collectionId <- item.collectionId
      canCreate <- access.canCreateInCollection(collectionId).toOption
      itemId <- itemService.insert(item)
    } yield itemId
  }

  override def canCreateInCollection(collectionId: String)(identity: OrgAndOpts): Validation[V2Error, Boolean] = {
    access.canCreateInCollection(collectionId)(identity)
  }

  private def canWithPermission(itemId: String, p: Permission)(implicit identity: OrgAndOpts): Validation[V2Error, Item] = {
    logger.trace(s"can ${p.name} to $itemId")

    def loadItem = for {
      vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
      item <- itemService.findOneById(vid).toSuccess(cantFindItemWithId(vid))
      //ensure the item has v2 data
      updatedItem <- Success(itemTransformer.updateV2Json(item))
    } yield updatedItem

    for {
      item <- loadItem
      granted <- access.grant(identity, p, item)
    } yield {
      logger.trace(s"accessGranted=$granted")
      item
    }
  }
}
