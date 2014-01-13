package org.corespring.v2player.integration.actionBuilders

import org.bson.types.ObjectId
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.User
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.services.UserService
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.actionBuilders.CheckUserAndPermissions.Errors
import org.corespring.v2player.integration.actionBuilders.access.{PlayerOptions, V2PlayerCookieReader}
import org.corespring.v2player.integration.securesocial.SecureSocialService
import play.api.mvc.{Action, Result, AnyContent, Request}


object CheckUserAndPermissions {

  import play.api.http.Status._

  object Errors {

    val default = (UNAUTHORIZED, "Failed to grant access")

    def cantLoadSession(id: String) = (NOT_FOUND, s"Can't load session with id $id")

    val cantParseItemId = (BAD_REQUEST, "Can't parse itemId")

    def cantFindItemWithId(vid: VersionedId[ObjectId]) = cantFindById("item", vid.toString())

    def cantFindOrgWithId(orgId: ObjectId) = cantFindById("org", orgId.toString)

    def cantFindById(name: String, id: String) = (NOT_FOUND, s"Can't find $name with id $id")
  }

}

abstract class AuthenticatedSessionActionsCheckUserAndPermissions(
                                                                   secureSocialService: SecureSocialService,
                                                                   userService: UserService,
                                                                   sessionService: MongoService,
                                                                   itemService: ItemService,
                                                                   orgService: OrganizationService) extends AuthenticatedSessionActions with V2PlayerCookieReader {

  import play.api.http.Status._
  import play.api.mvc.Results._
  import scalaz.Scalaz._
  import scalaz._

  private def userFromSession(request: Request[AnyContent]): Option[User] = {

    for {
      ssUser <- secureSocialService.currentUser(request)
      dbUser <- userService.getUser(ssUser.identityId.userId, ssUser.identityId.providerId)
    } yield dbUser
  }

  private def orgCanAccessSession(sessionId: String, orgId: ObjectId): Validation[(Int, String), Boolean] = for {
    session <- sessionService.load(sessionId).toSuccess(Errors.cantLoadSession(sessionId))
    itemId <- (session \ "itemId").asOpt[String].toSuccess(Errors.cantParseItemId)
    canAccess <- orgCanAccessItem(itemId, orgId)
  } yield canAccess


  private def orgCanAccessItem(itemId: String, orgId: ObjectId): Validation[(Int, String), Boolean] = {
    def canAccess(collectionId: String) = orgService.canAccessCollection(orgId, new ObjectId(collectionId), Permission.Read)

    for {
      vid <- VersionedId(itemId).toSuccess(Errors.cantParseItemId)
      item <- itemService.findOneById(vid).toSuccess(Errors.cantFindItemWithId(vid))
      org <- orgService.findOneById(orgId).toSuccess(Errors.cantFindOrgWithId(orgId))
      canAccess <- if (canAccess(item.collectionId)) Success(true) else Failure(Errors.default)
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
        case Success(false) => Unauthorized("Authentication failed")
        case Failure(error) => Status(error._1)(error._2)
      }
  }

  def createSessionHandleNotAuthorized(itemId: String)(authorized: (Request[AnyContent]) => Result)(failed: (Request[AnyContent], Int, String) => Result): Action[AnyContent] = Action {
    request =>
      checkAccess(itemId, request, orgCanAccessItem _) match {
        case Success(true) => authorized(request)
        case Success(false) => failed(request, BAD_REQUEST, "Didn't work")
        case Failure(error) => failed(request, error._1, error._2)
      }
  }

  private def checkAccess(sessionId: String, request: Request[AnyContent], getOrgAccess: (String, ObjectId) => Validation[(Int, String), Boolean]) = {
    getOrgIdAndOptions(request).map {
      t: (ObjectId, PlayerOptions) =>
        val (orgId, options) = t
        for {
          orgAccess <- getOrgAccess(sessionId, orgId)
          permissionAccess <- hasPermissions(sessionId, options).leftMap(msg => (UNAUTHORIZED, msg))
        } yield permissionAccess
    }.getOrElse(Failure(Errors.default))
  }
}
