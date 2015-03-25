package org.corespring.v2.auth.wired

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.auth.{Auth, ItemAuth}
import org.corespring.v2.auth.models.{Mode, OrgAndOpts, PlayerAccessSettings}
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory

import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

trait Access[DATA,REQUESTER] {
  def can(identity:REQUESTER, permission:Permission, data: DATA)  : Validation[V2Error,Boolean]
}

trait ItemAccess extends Access[Item,OrgAndOpts] {

  lazy val logger = V2LoggerFactory.getLogger("auth.ItemAccess")
  def hasPermissions(itemId: String, settings: PlayerAccessSettings): Validation[V2Error, Boolean]

  def orgService : OrganizationService

  override def can(identity: OrgAndOpts, permission: Permission, item: Item): Validation[V2Error,Boolean] = {
    def canAccess(collectionId: String) = orgService.canAccessCollection(identity.org,new ObjectId(collectionId),permission)

    val out = for {
      collectionId <- item.collectionId.toSuccess(generalError(s"The item ${item.id} has no collectionId"))
      canAccess <- if (canAccess(collectionId))
        Success(true)
      else
        Failure(orgCantAccessCollection(identity.org.id, item.collectionId.getOrElse("?"), permission.name))
      permissionAccess <- hasPermissions(item.id.toString, identity.opts)
    } yield {
      logger.trace(s"orgCanAccessItem: $canAccess")
      canAccess && permissionAccess
    }
  }
}

trait ItemDraftAccess extends Access[ItemDraft,OrgAndOpts] {
  def itemAccess : ItemAccess
  override def can(identity: OrgAndOpts, permission: Permission, data: ItemDraft): Validation[V2Error,Boolean] = {
    itemAccess.can(identity, permission, data.src.data)
  }
}

trait AItemAuthWired extends ItemAuth[OrgAndOpts] {

  lazy val logger = V2LoggerFactory.getLogger("auth.ItemAuth")

  def hasPermissions(itemId: String, settings: PlayerAccessSettings): Validation[V2Error, Boolean]

  override def loadForRead(itemId: String)(implicit identity: OrgAndOpts): Validation[V2Error, Item] = canWithPermission(itemId, Permission.Read)

  override def loadForWrite(itemId: String)(implicit identity: OrgAndOpts): Validation[V2Error, Item] = canWithPermission(itemId, Permission.Write)

  private def canWithPermission(itemId: String, p: Permission)(implicit identity: OrgAndOpts): Validation[V2Error, Item] = {
    logger.trace(s"can ${p.name} to $itemId")

    def canAccess(collectionId: String) = orgService.canAccessCollection(identity.org,new ObjectId(collectionId),p)

    def loadItem = for {
      vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
      item <- itemService.findOneById(vid).toSuccess(cantFindItemWithId(vid))
      //ensure the item has v2 data
      updatedItem <- Success(itemTransformer.updateV2Json(item))
    } yield updatedItem

    for {
      item <- loadItem
      collectionId <- item.collectionId.toSuccess(generalError(s"The item ${itemId} has no collectionId"))
      canAccess <- if (canAccess(collectionId))
        Success(true)
      else
        Failure(orgCantAccessCollection(identity.org.id, item.collectionId.getOrElse("?"), p.name))
      permissionAccess <- hasPermissions(itemId, identity.opts)
    } yield {
      logger.trace(s"orgCanAccessItem: $canAccess")
      item
    }
  }

  override def save(item: Item, createNewVersion: Boolean)(implicit identity: OrgAndOpts): Unit = {
    loadForWrite(item.id.toString) match {
      case Success(dbItem) => itemService.save(item, createNewVersion)
      case Failure(msg) => throw new RuntimeException(s"Error saving $msg")
    }
  }

  override def insert(item: Item)(implicit identity: OrgAndOpts): Option[VersionedId[ObjectId]] = {
    for {
      collectionId <- item.collectionId
      can <- canCreateInCollection(collectionId).toOption
      itemId <- itemService.insert(item)
    } yield itemId
  }
}
trait ItemAuthWired extends ItemAuth[OrgAndOpts] {

  lazy val logger = V2LoggerFactory.getLogger("auth.ItemAuth")

  def orgService: OrganizationService

  def itemService: ItemService

  def itemTransformer: ItemTransformer

  def hasPermissions(itemId: String, settings: PlayerAccessSettings): Validation[V2Error, Boolean]

  override def canCreateInCollection(collectionId: String)(implicit identity: OrgAndOpts): Validation[V2Error, Boolean] = {

    def canWrite(orgId: ObjectId, collectionId: ObjectId) = {
      if (orgService.canAccessCollection(orgId, collectionId, Permission.Write)) {
        Success(true)
      } else {
        Failure(orgCantAccessCollection(orgId, collectionId.toString, Permission.Write.name))
      }
    }

    canWrite(identity.org.id, new ObjectId(collectionId))
  }

  override def loadForRead(itemId: String)(implicit identity: OrgAndOpts): Validation[V2Error, Item] = canWithPermission(itemId, Permission.Read)

  override def loadForWrite(itemId: String)(implicit identity: OrgAndOpts): Validation[V2Error, Item] = canWithPermission(itemId, Permission.Write)

  private def canWithPermission(itemId: String, p: Permission)(implicit identity: OrgAndOpts): Validation[V2Error, Item] = {
    logger.trace(s"can ${p.name} to $itemId")
    
    def canAccess(collectionId: String) = orgService.canAccessCollection(identity.org,new ObjectId(collectionId),p)

    def loadItem = for {
        vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
        item <- itemService.findOneById(vid).toSuccess(cantFindItemWithId(vid))
        //ensure the item has v2 data
        updatedItem <- Success(itemTransformer.updateV2Json(item))
      } yield updatedItem

    for {
      item <- loadItem
      collectionId <- item.collectionId.toSuccess(generalError(s"The item ${itemId} has no collectionId"))
      canAccess <- if (canAccess(collectionId))
        Success(true)
      else
        Failure(orgCantAccessCollection(identity.org.id, item.collectionId.getOrElse("?"), p.name))
      permissionAccess <- hasPermissions(itemId, identity.opts)
    } yield {
      logger.trace(s"orgCanAccessItem: $canAccess")
      item
    }
  }

  override def save(item: Item, createNewVersion: Boolean)(implicit identity: OrgAndOpts): Unit = {
    loadForWrite(item.id.toString) match {
      case Success(dbItem) => itemService.save(item, createNewVersion)
      case Failure(msg) => throw new RuntimeException(s"Error saving $msg")
    }
  }

  override def insert(item: Item)(implicit identity: OrgAndOpts): Option[VersionedId[ObjectId]] = {
    for {
      collectionId <- item.collectionId
      can <- canCreateInCollection(collectionId).toOption
      itemId <- itemService.insert(item)
    } yield itemId
  }
}
