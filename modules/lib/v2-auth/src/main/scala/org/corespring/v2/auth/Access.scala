package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.drafts.item.models.ItemDraft
import org.corespring.platform.core.models.ContentCollection
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.v2.auth.models.{ Mode, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory

import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

trait Access[DATA, REQUESTER] {
  def grant(identity: REQUESTER, permission: Permission, data: DATA): Validation[V2Error, Boolean]

  def hasPermissions(itemId: String, sessionId: Option[String], settings: PlayerAccessSettings): Validation[V2Error, Boolean] = {
    AccessSettingsWildcardCheck.allow(itemId, sessionId, Mode.evaluate, settings)
  }
}

trait ItemAccess extends Access[Item, OrgAndOpts] {

  lazy val logger = V2LoggerFactory.getLogger("auth.ItemAccess")

  def orgService: OrganizationService

  def canCreateInCollection(collectionId: String)(implicit identity: OrgAndOpts): Validation[V2Error, Boolean] = {

    def canWrite(orgId: ObjectId, collectionId: ObjectId) = {
      if (orgService.canAccessCollection(orgId, collectionId, Permission.Write)) {
        Success(true)
      } else {
        Failure(orgCantAccessCollection(orgId, collectionId.toString, Permission.Write.name))
      }
    }

    for {
      oid <- try {
        Success(new ObjectId(collectionId))
      } catch {
        case t: Throwable => Failure(invalidObjectId(collectionId, "collectionId"))
      }
      success <- canWrite(identity.org.id, new ObjectId(collectionId))
    } yield success
  }

  override def grant(identity: OrgAndOpts, permission: Permission, item: Item): Validation[V2Error, Boolean] = {

    def orgCanAccess(collectionId: String) = orgService.canAccessCollection(identity.org, new ObjectId(collectionId), permission)
    def isArchived(collectionId: String) = collectionId == ContentCollection.archiveCollId.toString

    for {
      collectionId <- item.collectionId.toSuccess(noCollectionIdForItem(item.id))
      canAccess <- if (orgCanAccess(collectionId) || isArchived(collectionId))
        Success(true)
      else
        Failure(orgCantAccessCollection(identity.org.id, item.collectionId.getOrElse("?"), permission.name))
      permissionAccess <- hasPermissions(item.id.toString, None, identity.opts)
    } yield {
      logger.trace(s"orgCanAccessItem: $canAccess")
      canAccess && permissionAccess
    }
  }
}

