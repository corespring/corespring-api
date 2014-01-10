package org.corespring.poc.integration.impl.actionBuilders

import org.bson.types.ObjectId
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.User
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.itemSession.ItemSession
import org.corespring.platform.core.services.UserService
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.poc.integration.impl.actionBuilders.access.{PlayerOptions, V2PlayerCookieReader}
import org.corespring.poc.integration.impl.securesocial.SecureSocialService
import play.api.mvc.{Action, Result, AnyContent, Request}


abstract class AuthenticatedSessionActionsImpl(
                                                secureSocialService: SecureSocialService,
                                                userService: UserService,
                                                sessionService: MongoService,
                                                itemService: ItemService,
                                                orgService: OrganizationService) extends AuthenticatedSessionActions with V2PlayerCookieReader {

  import play.api.mvc.Results._
  import scalaz.Scalaz._
  import scalaz._

  private def userFromSession(request: Request[AnyContent]): Option[User] = {

    for {
      ssUser <- secureSocialService.currentUser(request)
      dbUser <- userService.getUser(ssUser.identityId.userId, ssUser.identityId.providerId)
    } yield dbUser
  }

  private def orgCanAccessSession(sessionId: String, orgId: ObjectId): Validation[String, Boolean] = for {
    session <- sessionService.load(sessionId).toSuccess(s"Can't load session $sessionId")
    itemId <- (session \ "itemId").asOpt[String].toSuccess(s"Can't parse itemId")
    canAccess <- orgCanAccessItem(itemId, orgId)
  } yield canAccess


  private def orgCanAccessItem(itemId: String, orgId: ObjectId): Validation[String, Boolean] = {
    def canAccess(collectionId: String) = orgService.canAccessCollection(orgId, new ObjectId(collectionId), Permission.Read)

    for {
      vid <- VersionedId(itemId).toSuccess(s"Can't parse itemId")
      item <- itemService.findOneById(vid).toSuccess(s"Can't find item with id $vid")
      org <- orgService.findOneById(orgId).toSuccess(s"Can't find org with id ${orgId}")
      canAccess <- if (canAccess(item.collectionId)) Success(true) else Failure("Can't access")
    } yield canAccess
  }

  def getOrgIdAndOptions(request: Request[AnyContent]): Option[(ObjectId, PlayerOptions)] = userFromSession(request).map(
    u =>
      (u.org.orgId, PlayerOptions.ANYTHING)
  ) orElse anonymousUser(request)

  private def anonymousUser(request: Request[AnyContent]): Option[(ObjectId, PlayerOptions)] = for {
    orgId <- orgIdFromCookie(request)
    options <- renderOptions(request)
  } yield (new ObjectId(orgId), options)

  def hasPermissions(sessionId: String, options: PlayerOptions): Validation[String, Boolean]

  def read(sessionId: String)(block: (Request[AnyContent]) => Result): Action[AnyContent] = Action {
    request =>
      checkAccess(sessionId, request, orgCanAccessSession _) match {
        case Success(true) => block(request)
        case Success(false) => Unauthorized("..")
        case Failure(msg) => Unauthorized("..")
      }
  }

  def createSessionHandleNotAuthorized(itemId: String)(authorized: (Request[AnyContent]) => Result)(notAuthorized: (Request[AnyContent], String) => Result): Action[AnyContent] = Action {
    request =>
      checkAccess(itemId, request, orgCanAccessItem _) match {
        case Success(true) => authorized(request)
        case Success(false) => notAuthorized(request, "Didn't work")
        case Failure(msg) => notAuthorized(request, msg)
      }
  }

  private def checkAccess(sessionId: String, request: Request[AnyContent], getOrgAccess: (String, ObjectId) => Validation[String, Boolean]) = {
    getOrgIdAndOptions(request).map {
      t: (ObjectId, PlayerOptions) =>
        val (orgId, options) = t
        for {
          orgAccess <- getOrgAccess(sessionId, orgId)
          permissionAccess <- hasPermissions(sessionId, options)
        } yield permissionAccess
    }.getOrElse(Failure("Failed to grant access"))
  }
}
