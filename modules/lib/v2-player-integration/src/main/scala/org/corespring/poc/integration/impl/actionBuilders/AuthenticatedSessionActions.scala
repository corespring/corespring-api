package org.corespring.poc.integration.impl.actionBuilders

import org.bson.types.ObjectId
import org.corespring.platform.core.models.UserOrg
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.itemSession.ItemSessionCompanion
import org.corespring.platform.core.services.UserService
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.poc.integration.impl.securesocial.SecureSocialService
import play.api.mvc.{Action, Request, Result, AnyContent}
import scalaz.Scalaz._
import scalaz._
import securesocial.core.Identity
import org.corespring.mongo.json.services.MongoService
import play.api.libs.json.JsValue

trait AuthenticatedSessionActions {
  def read(id: String)(block: Request[AnyContent] => Result): Action[AnyContent]

  def defaultNotAuthorized(request: Request[AnyContent], msg: String): Result = {
    import play.api.mvc.Results._
    Unauthorized(msg)
  }

  def createSession(id: String)(authorized: (Request[AnyContent]) => Result): Action[AnyContent] = createSessionHandleNotAuthorized(id)(authorized)(defaultNotAuthorized)

  /**
   * Optionally call create session and pass in a handler for not authorized
   */
  def createSessionHandleNotAuthorized(id: String)(authorized: (Request[AnyContent]) => Result)(notAuthorized: (Request[AnyContent], String) => Result): Action[AnyContent]
}


class AuthenticatedSessionActionsImpl(
                                       val itemService : ItemService,
                                       val orgService : OrganizationService,
                                       val sessionService : MongoService,
                                       val userService: UserService,
                                       val secureSocialService: SecureSocialService)
  extends AuthenticatedSessionActions with OrgToItemAccessControl {

  import play.api.mvc.Results._

  override def createSessionHandleNotAuthorized(id: String)(authorized: (Request[AnyContent]) => Result)(notAuthorized: (Request[AnyContent], String) => Result): Action[AnyContent] = Action {
    request =>

      val result = for {
        uo <- userOrgFromRequest(request).toSuccess("Can't load user session in request")
        vid <- VersionedId(id).toSuccess(s"Can't parse id: $id")
        canAccess <- orgCanAccessItem(Permission.Read, uo, vid)
      } yield {
        canAccess
      }

      result match {
        case Success(true) => authorized(request)
        case Success(false) => notAuthorized(request, "An unknown authorization error occured")
        case Failure(msg) => notAuthorized(request, msg)
      }
  }

  override def read(sessionId: String)(block: (Request[AnyContent]) => Result): Action[AnyContent] = Action {
    request =>

      def getItemId(session:JsValue) : Option[VersionedId[ObjectId]] =  (session \ "itemId").asOpt[String].map( VersionedId(_)).flatten

      val validationResult: Validation[String, Boolean] = for {
        u <- secureSocialService.currentUser(request).toSuccess("No user")
        o <- getUserOrg(u).toSuccess(s"No user org found for: ${u.identityId.userId}")
        session <- sessionService.load(sessionId).toSuccess(s"Can't find session with id $sessionId")
        itemId <- getItemId( session ).toSuccess("Can't find or parse item Id" )
        canAccess <- orgCanAccessItem(Permission.Read,o, itemId)
      } yield {
        canAccess
      }

      validationResult match {
        case Success(hasAccess) => if(hasAccess) block(request) else Unauthorized("Access denied")
        case Failure(e) => Unauthorized(e)
      }
  }

  private def getUserOrg(id: Identity): Option[UserOrg] = userService.getUser(id.identityId.userId, id.identityId.providerId).map(_.org)

}
