package org.corespring.v2player.integration.actionBuilders

import org.bson.types.ObjectId
import org.corespring.common.log.ClassLogging
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.services.UserService
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.actionBuilders.CheckUserAndPermissions.Errors
import org.corespring.v2player.integration.actionBuilders.access.PlayerOptions
import org.corespring.v2player.integration.securesocial.SecureSocialService
import play.api.mvc.{ AnyContent, Request }
import org.corespring.v2player.integration.actionBuilders.access.Mode.Mode
import org.corespring.platform.core.models.auth.Permission

abstract class BaseAuth(
  val secureSocialService: SecureSocialService,
  val userService: UserService,
  sessionService: MongoService,
  itemService: ItemService,
  orgService: OrganizationService)
  extends LoadOrgAndOptions
  with ClassLogging {

  import play.api.http.Status._
  import scalaz.Scalaz._
  import scalaz._

  override def loggerName = "org.corespring.v2player.integration.actionBuilders.AuthenticatedSessionActionsCheckUserAndPermissions"

  def hasPermissions(itemId: String, sessionId: Option[String], mode: Mode, options: PlayerOptions): Validation[String, Boolean]

  protected def checkAccess(
    request: Request[AnyContent],
    getOrgAccess: ObjectId => Validation[(Int, String), Boolean],
    getPermissions: (PlayerOptions) => Validation[String, Boolean]) = {
    {
      logger.trace(s"checkAccess: ${request.path}")
      getOrgIdAndOptions(request).map {
        t: (ObjectId, PlayerOptions) =>
          logger.debug(s"checkAccess (orgId,options): $t}")
          val (orgId, options) = t
          for {
            orgAccess <- getOrgAccess(orgId)
            permissionAccess <- getPermissions(options).leftMap(msg => (UNAUTHORIZED, msg))
          } yield permissionAccess
      }.getOrElse(Failure(Errors.noOrgIdAndOptions(request)))
    }
  }

  protected def orgCanAccessItem(itemId: String, orgId: ObjectId): Validation[(Int, String), Boolean] = {
    def canAccess(collectionId: String) = orgService.canAccessCollection(orgId, new ObjectId(collectionId), Permission.Read)

    for {
      vid <- VersionedId(itemId).toSuccess(Errors.cantParseItemId)
      item <- itemService.findOneById(vid).toSuccess(Errors.cantFindItemWithId(vid))
      org <- orgService.findOneById(orgId).toSuccess(Errors.cantFindOrgWithId(orgId))
      canAccess <- if (canAccess(item.collectionId)) Success(true) else Failure(Errors.orgCantAccessCollection(orgId, item.collectionId))
    } yield {
      logger.trace(s"orgCanAccessItem: $canAccess")
      canAccess
    }
  }

}

