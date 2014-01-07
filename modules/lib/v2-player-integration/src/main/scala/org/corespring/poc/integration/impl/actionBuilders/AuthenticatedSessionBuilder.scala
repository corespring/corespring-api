package org.corespring.poc.integration.impl.actionBuilders

import org.bson.types.ObjectId
import org.corespring.platform.core.models.UserOrg
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.itemSession.ItemSessionCompanion
import org.corespring.platform.core.services.UserService
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.poc.integration.impl.securesocial.SecureSocialService
import play.api.mvc.{Action, Request, Result, AnyContent}
import scalaz.Scalaz._
import scalaz._
import securesocial.core.Identity

trait AuthenticatedSessionBuilder {
  def read(id: String)(block: Request[AnyContent] => Result): Action[AnyContent]
}

class AuthenticatedSessionBuilderImpl(
                                       itemService : ItemService,
                                       orgService : OrganizationService,
                                       sessionService : ItemSessionCompanion,
                                       userService: UserService,
                                       secureSocialService: SecureSocialService) extends AuthenticatedSessionBuilder {

  import play.api.mvc.Results._


  def read(sessionId: String)(block: (Request[AnyContent]) => Result): Action[AnyContent] = Action {
    request =>

      val validationResult: Validation[String, Boolean] = for {
        u <- secureSocialService.currentUser(request).toSuccess("No user")
        o <- getUserOrg(u).toSuccess(s"No user org found for: ${u.identityId.userId}")
        canAccess <- canAccessSession(Permission.Read)(o, sessionId)
      } yield {
        canAccess
      }

      validationResult match {
        case Success(hasAccess) => if(hasAccess) block(request) else Unauthorized("Access denied")
        case Failure(e) => Unauthorized(e)
      }
  }

  private def getUserOrg(id: Identity): Option[UserOrg] = userService.getUser(id.identityId.userId, id.identityId.providerId).map(_.org)

  private def canAccessSession(requestedPermission:Permission)(userOrg: UserOrg, sessionId: String): Validation[String, Boolean] = for {
      orgPermission <- Permission.fromLong(userOrg.pval).toSuccess("Can't parse pval")
      hasPermission <- if(orgPermission.has(requestedPermission)) Success(true) else Failure("Permission test failed")
      org <- orgService.findOneById(userOrg.orgId).toSuccess(s"Cant find org with id: ${userOrg.orgId}")
      session <- sessionService.findOneById(new ObjectId(sessionId)).toSuccess(s"can't find session with id: $sessionId")
      item <- itemService.findOneById(session.itemId).toSuccess(s"Can't find item with id: ${session.itemId}")
      canAccessCollection <- if(orgService.canAccessCollection(org.id, new ObjectId(item.collectionId), requestedPermission)) Success(true) else Failure("Can't access collection")
    } yield true

}
