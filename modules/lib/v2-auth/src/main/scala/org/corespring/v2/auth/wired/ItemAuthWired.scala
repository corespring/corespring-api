package org.corespring.v2.auth.wired

import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.models.{ OrgAndOpts, PlayerOptions }
import org.corespring.v2.auth.{ ItemAuth, LoadOrgAndOptions }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory

import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

trait ItemAuthWired extends ItemAuth[OrgAndOpts] with LoadOrgAndOptions {

  lazy val logger = V2LoggerFactory.getLogger("auth.ItemAuth")

  def orgService: OrganizationService

  def itemService: ItemService

  def hasPermissions(itemId: String, options: PlayerOptions): Validation[V2Error, Boolean]

  override def canCreateInCollection(collectionId: String)(implicit identity: OrgAndOpts): Validation[V2Error, Boolean] = {

    def write(orgId: ObjectId, collectionId: ObjectId) = {
      if (orgService.canAccessCollection(orgId, collectionId, Permission.Write)) {
        Success(true)
      } else {
        Failure(orgCantAccessCollection(orgId, collectionId.toString, Permission.Write.name))
      }
    }

    write(identity.orgId, new ObjectId(collectionId))
  }

  override def loadForRead(itemId: String)(implicit identity: OrgAndOpts): Validation[V2Error, Item] = canWithPermission(itemId, Permission.Read)

  override def loadForWrite(itemId: String)(implicit identity: OrgAndOpts): Validation[V2Error, Item] = canWithPermission(itemId, Permission.Write)

  private def canWithPermission(itemId: String, p: Permission)(implicit identity: OrgAndOpts): Validation[V2Error, Item] = {
    logger.trace(s"can ${p.name} to $itemId")

    def canAccess(collectionId: String) = orgService.canAccessCollection(identity.orgId, new ObjectId(collectionId), p)

    for {
      vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
      item <- itemService.findOneById(vid).toSuccess(cantFindItemWithId(vid))
      org <- orgService.findOneById(identity.orgId).toSuccess(cantFindOrgWithId(identity.orgId))
      canAccess <- if (canAccess(item.collectionId.getOrElse("?")))
        Success(true)
      else
        Failure(orgCantAccessCollection(identity.orgId, item.collectionId.getOrElse("?"), Permission.Write.name))
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
