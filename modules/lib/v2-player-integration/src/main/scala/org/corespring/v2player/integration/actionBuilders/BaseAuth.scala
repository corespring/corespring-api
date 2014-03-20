package org.corespring.v2player.integration.actionBuilders

import org.bson.types.ObjectId
import org.corespring.common.log.ClassLogging
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.services.UserService
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.actionBuilders.access.Mode.Mode
import org.corespring.v2player.integration.actionBuilders.access.PlayerOptions
import org.corespring.v2player.integration.errors.Errors._
import org.corespring.v2player.integration.errors.V2Error
import org.corespring.v2player.integration.securesocial.SecureSocialService
import play.api.mvc.{ AnyContent, Request }
import org.slf4j.LoggerFactory

abstract class BaseAuth(
  val secureSocialService: SecureSocialService,
  val userService: UserService,
  sessionService: MongoService,
  itemService: ItemService,
  orgService: OrganizationService)
  extends LoadOrgAndOptions
  with ClassLogging {

  import scalaz.Scalaz._
  import scalaz._

  override protected lazy val logger = LoggerFactory.getLogger("v2player.base-auth")

  override def loggerName = "org.corespring.v2player.integration.actionBuilders.AuthenticatedSessionActionsCheckUserAndPermissions"

  def hasPermissions(itemId: String, sessionId: Option[String], mode: Mode, options: PlayerOptions): Validation[String, Boolean]

  protected def checkAccess(
    request: Request[AnyContent],
    getOrgAccess: ObjectId => Validation[V2Error, Boolean],
    getPermissions: (PlayerOptions) => Validation[String, Boolean]): Validation[V2Error, Boolean] = {
    {
      logger.trace(s"checkAccess: ${request.path}")
      getOrgIdAndOptions(request).map {
        t: (ObjectId, PlayerOptions) =>
          logger.debug(s"checkAccess (orgId,options): $t}")
          val (orgId, options) = t

          import play.api.http.Status._

          for {
            orgAccess <- getOrgAccess(orgId)
            permissionAccess <- getPermissions(options).leftMap(msg => generalError(UNAUTHORIZED, msg))
          } yield permissionAccess
      }.getOrElse(Failure(noOrgIdAndOptions(request)))
    }
  }

  protected def orgCanAccessItem(itemId: String, orgId: ObjectId): Validation[V2Error, Boolean] = {
    def canAccess(collectionId: String) = orgService.canAccessCollection(orgId, new ObjectId(collectionId), Permission.Read)

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

}

