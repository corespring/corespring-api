package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.models.{ Organization, User }
import org.corespring.services.{ OrganizationService, UserService }
import org.corespring.v2.auth.models.AuthMode.AuthMode
import org.corespring.v2.auth.models.{ AuthMode, PlayerAccessSettings }
import org.corespring.v2.errors.Errors.{ cantFindOrgWithId, noUserSession }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.warnings.V2Warning
import org.corespring.web.user.{ SecureSocial, UserFromRequest }
import play.api.mvc._

import scalaz.Scalaz._
import scalaz.Validation

case class UserSessionIdentityInput(input: User) extends Input[User] {
  override def playerAccessSettings: PlayerAccessSettings = PlayerAccessSettings.ANYTHING

  override def warnings: Seq[V2Warning] = Nil

  override def authMode: AuthMode = AuthMode.UserSession

  override def apiClientId: Option[ObjectId] = None
}

class UserSessionOrgIdentity(
  orgService: OrganizationService,
  val userService: UserService,
  val secureSocial: SecureSocial) extends OrgAndOptsIdentity[User]
  with UserFromRequest {

  override val name = "user-session-cookie"

  override def toOrgAndUser(i: Input[User]): Validation[V2Error, (Organization, Option[User])] = {
    orgService.findOneById(i.input.org.orgId).toSuccess(cantFindOrgWithId(i.input.org.orgId)).map { o =>
      o -> Some(i.input)
    }
  }

  override def toInput(rh: RequestHeader): Validation[V2Error, Input[User]] = {
    userFromSession(rh)
      .toSuccess(noUserSession(rh))
      .map(UserSessionIdentityInput(_))
  }
}

