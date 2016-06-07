package org.corespring.v2.auth.wired

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.item.Item
import org.corespring.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.{ ItemAccess, ItemAuth }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import play.api.Logger

import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

class ItemAuthWired(
  itemService: ItemService,
  itemTransformer: ItemTransformer,
  access: ItemAccess) extends ItemAuth[OrgAndOpts] {

  lazy val logger = Logger(classOf[ItemAuthWired])

  override def loadForRead(itemId: String)(implicit identity: OrgAndOpts): Validation[V2Error, Item] = canWithPermission(itemId, Permission.Read)

  override def loadForWrite(itemId: String)(implicit identity: OrgAndOpts): Validation[V2Error, Item] = canWithPermission(itemId, Permission.Write)

  override def save(item: Item, createNewVersion: Boolean)(implicit identity: OrgAndOpts): Unit = {
    loadForWrite(item.id.toString) match {
      case Success(dbItem) => itemService.save(item, createNewVersion)
      case Failure(msg) => throw new RuntimeException(s"Error saving $msg")
    }
  }

  override def canWrite(id: String)(implicit identity: OrgAndOpts): Validation[V2Error, Boolean] = {
    loadForWrite(id).rightMap { i => true }
  }

  override def delete(id: String)(implicit identity: OrgAndOpts): Validation[V2Error, VersionedId[ObjectId]] = {
    loadForWrite(id).map { item =>
      itemService.moveItemToArchive(item.id)
      item.id
    }
  }

  override def insert(item: Item)(implicit identity: OrgAndOpts): Option[VersionedId[ObjectId]] = {
    for {
      canCreate <- access.canCreateInCollection(item.collectionId).toOption
      itemId <- itemService.insert(item)
    } yield itemId
  }

  override def canCreateInCollection(collectionId: String)(identity: OrgAndOpts): Validation[V2Error, Boolean] = {
    access.canCreateInCollection(collectionId)(identity)
  }

  private def canWithPermission(itemId: String, p: Permission)(implicit identity: OrgAndOpts): Validation[V2Error, Item] = {
    logger.trace(s"can ${p.name} to $itemId")

    for {
      item <- loadItem(itemId)
      granted <- access.grant(identity, p, item)
    } yield {
      logger.trace(s"accessGranted=$granted")
      item
    }
  }

  private def loadItem(itemId: String) = for {
    vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
    item <- itemService.findOneById(vid).toSuccess(cantFindItemWithId(vid))
    //ensure the item has v2 data
    updatedItem <- Success(itemTransformer.updateV2Json(item))
  } yield updatedItem

}
