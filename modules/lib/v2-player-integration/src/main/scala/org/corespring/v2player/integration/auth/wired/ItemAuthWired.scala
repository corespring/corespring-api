package org.corespring.v2player.integration.auth.wired

import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.auth.{ ItemAuth, LoadOrgAndOptions }
import org.corespring.v2player.integration.cookies.Mode._
import org.corespring.v2player.integration.cookies.PlayerOptions
import org.corespring.v2player.integration.errors.Errors._
import org.corespring.v2player.integration.errors.V2Error
import org.slf4j.LoggerFactory
import play.api.http.Status._
import play.api.mvc.RequestHeader

import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

trait ItemAuthWired extends ItemAuth with LoadOrgAndOptions {

  lazy val logger = LoggerFactory.getLogger("v2.auth.ItemAuth")

  def orgService: OrganizationService

  def itemService: ItemService

  def hasPermissions(itemId: String, sessionId: Option[String], mode: Mode, options: PlayerOptions): Validation[String, Boolean]

  override def canCreateInCollection(collectionId: String)(implicit header: RequestHeader): Validation[String, Boolean] = {

    def write(orgId: ObjectId, collectionId: ObjectId): Validation[String, Boolean] = {
      if (orgService.canAccessCollection(orgId, collectionId, Permission.Write)) {
        Success(true)
      } else {
        Failure(orgCantAccessCollection(orgId, collectionId.toString).message)
      }
    }

    val out: Validation[String, Boolean] = for {
      (orgId, options) <- getOrgIdAndOptions(header)
      canWrite <- write(orgId, new ObjectId(collectionId))
    } yield canWrite
    out
  }

  override def canRead(itemId: String)(implicit header: RequestHeader): Validation[String, Boolean] = {
    canWithPermission(itemId, Permission.Read).leftMap(_.message)
  }

  override def canWrite(itemId: String)(implicit header: RequestHeader): Validation[String, Boolean] = {
    canWithPermission(itemId, Permission.Write).leftMap(_.message)
  }

  private def canWithPermission(itemId: String, p: Permission)(implicit header: RequestHeader): Validation[V2Error, Boolean] = getOrgIdAndOptions(header).map { t: (ObjectId, PlayerOptions) =>

    logger.trace(s"can ${p.name} to $itemId")

    def checkOrgAccess(orgId: ObjectId): Validation[V2Error, Boolean] = {

      def canAccess(collectionId: String) = orgService.canAccessCollection(orgId, new ObjectId(collectionId), p)

      for {
        vid <- VersionedId(itemId).toSuccess(cantParseItemId)
        item <- itemService.findOneById(vid).toSuccess(cantFindItemWithId(vid))
        org <- orgService.findOneById(orgId).toSuccess(cantFindOrgWithId(orgId))
        canAccess <- if (canAccess(item.collectionId.getOrElse("?"))) Success(true) else Failure(orgCantAccessCollection(orgId, item.collectionId.getOrElse("?")))
      } yield {
        logger.trace(s"orgCanAccessItem: $canAccess")
        canAccess
      }
    }

    val (orgId, options) = t
    logger.debug(s"checkAccess (orgId,options): $t}")

    for {
      orgAccess <- checkOrgAccess(orgId)
      permissionAccess <- hasPermissions(itemId, None, evaluate, options).leftMap(msg => generalError(UNAUTHORIZED, msg))
    } yield permissionAccess
  }.getOrElse(Failure(noOrgIdAndOptions(header)))

}
