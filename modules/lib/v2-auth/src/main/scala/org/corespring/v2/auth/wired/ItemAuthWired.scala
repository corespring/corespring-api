package org.corespring.v2.auth.wired

import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.models.Mode._
import org.corespring.v2.auth.models.PlayerOptions
import org.corespring.v2.auth.{ ItemAuth, LoadOrgAndOptions }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.slf4j.LoggerFactory
import play.api.mvc.RequestHeader

import scalaz.{ Failure, Success, Validation }
import scalaz.Scalaz._

trait ItemAuthWired extends ItemAuth with LoadOrgAndOptions {

  lazy val logger = LoggerFactory.getLogger("v2.auth.ItemAuth")

  def orgService: OrganizationService

  def itemService: ItemService

  def hasPermissions(itemId: String, sessionId: Option[String], mode: Mode, options: PlayerOptions): Validation[V2Error, Boolean]

  override def canCreateInCollection(collectionId: String)(implicit header: RequestHeader): Validation[V2Error, Boolean] = {

    def write(orgId: ObjectId, collectionId: ObjectId) = {
      if (orgService.canAccessCollection(orgId, collectionId, Permission.Write)) {
        Success(true)
      } else {
        Failure(orgCantAccessCollection(orgId, collectionId.toString))
      }
    }

    val out: Validation[V2Error, Boolean] = for {
      orgAndOpts <- getOrgIdAndOptions(header)
      canWrite <- write(orgAndOpts._1, new ObjectId(collectionId))
    } yield canWrite
    out
  }

  override def loadForRead(itemId: String)(implicit header: RequestHeader): Validation[V2Error, Item] = {
    canWithPermission(itemId, Permission.Read)
  }

  override def loadForWrite(itemId: String)(implicit header: RequestHeader): Validation[V2Error, Item] = {
    canWithPermission(itemId, Permission.Write)
  }

  private def canWithPermission(itemId: String, p: Permission)(implicit header: RequestHeader): Validation[V2Error, Item] = getOrgIdAndOptions(header) match {
    case Failure(e) => Failure(e)
    case Success((orgId, options)) => {

      logger.trace(s"can ${p.name} to $itemId")

      def checkOrgAccess(orgId: ObjectId): Validation[V2Error, Item] = {

        def canAccess(collectionId: String) = orgService.canAccessCollection(orgId, new ObjectId(collectionId), p)

        for {
          vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
          item <- itemService.findOneById(vid).toSuccess(cantFindItemWithId(vid))
          org <- orgService.findOneById(orgId).toSuccess(cantFindOrgWithId(orgId))
          canAccess <- if (canAccess(item.collectionId.getOrElse("?"))) Success(true) else Failure(orgCantAccessCollection(orgId, item.collectionId.getOrElse("?")))
        } yield {
          logger.trace(s"orgCanAccessItem: $canAccess")
          item
        }
      }

      logger.debug(s"checkAccess (orgId,options): $orgId -> $options")

      for {
        item <- checkOrgAccess(orgId)
        permissionAccess <- hasPermissions(itemId, None, evaluate, options)
      } yield item
    }
  }

  override def save(item: Item, createNewVersion: Boolean)(implicit header: RequestHeader): Unit = {
    loadForWrite(item.id.toString) match {
      case Success(dbItem) => itemService.save(item, createNewVersion)
      case Failure(msg) => throw new RuntimeException(s"Error saving $msg")
    }
  }

  override def insert(item: Item)(implicit header: RequestHeader): Option[VersionedId[ObjectId]] = {
    for {
      collectionId <- item.collectionId
      can <- canCreateInCollection(collectionId).toOption
      itemId <- itemService.insert(item)
    } yield itemId
  }
}
