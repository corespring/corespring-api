package org.corespring.v2.auth

import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.item.Item
import org.corespring.services.OrganizationService
import org.corespring.v2.auth.models.{ Mode, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error

import scalaz.{ Failure, Success, Validation }

trait Access[DATA, REQUESTER] {
  def grant(identity: REQUESTER, permission: Permission, data: DATA): Validation[V2Error, Boolean]

  def hasPermissions(itemId: String, sessionId: Option[String], settings: PlayerAccessSettings): Validation[V2Error, Boolean] = {
    AccessSettingsWildcardCheck.allow(itemId, sessionId, Mode.evaluate, settings)
  }
}

class ItemAccess(
  orgService: OrganizationService) extends Access[Item, OrgAndOpts] {

  lazy val logger = Logger(classOf[ItemAccess])

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

    for {
      canAccess <- if (orgCanAccess(item.collectionId))
        Success(true)
      else
        Failure(orgCantAccessCollection(identity.org.id, item.collectionId, permission.name))
      permissionAccess <- hasPermissions(item.id.toString, None, identity.opts)
    } yield {
      logger.trace(s"orgCanAccessItem: $canAccess")
      canAccess && permissionAccess
    }
  }
}

