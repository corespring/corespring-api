package developer.controllers

import com.typesafe.plugin._
import developer.DeveloperConfig
import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.legacy.ServiceLookup
import org.corespring.models.auth.Permission
import org.corespring.models.{ ContentCollection, Organization, User, UserOrg }
import play.api.Logger
import play.api.Play.current
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller, Result }
import securesocial.controllers.Registration.{ Success => RegistrationSuccess, _ }
import securesocial.core._
import securesocial.core.providers.utils._
import securesocial.core.providers.{ Token, UsernamePasswordProvider }

import scalaz.{ Failure, Success, Validation }

class MyRegistration(config: DeveloperConfig) extends Controller {

  case class MyRegistrationInfo(
    userName: Option[String],
    firstName: String,
    lastName: String,
    organization: Option[String],
    password: String)

  val formWithUsername = Form[MyRegistrationInfo](
    mapping(
      UserName -> nonEmptyText.verifying(Messages(UserNameAlreadyTaken), userName => {
        UserService.find(IdentityId(userName, providerId)).isEmpty
      }),
      FirstName -> nonEmptyText,
      LastName -> nonEmptyText,
      "organization" -> optional(nonEmptyText),
      Password ->
        tuple(
          Password1 -> nonEmptyText.verifying(
            use[PasswordValidator].errorMessage,
            p => use[PasswordValidator].isValid(p)),
          Password2 -> nonEmptyText).verifying(Messages(PasswordsDoNotMatch), passwords => passwords._1 == passwords._2)) // binding
          ((userName, firstName, lastName, organization, password) => MyRegistrationInfo(Some(userName), firstName, lastName, organization, password._1)) // unbinding
          (info => Some(info.userName.getOrElse(""), info.firstName, info.lastName, info.organization, ("", ""))))

  val formWithoutUsername = Form[MyRegistrationInfo](
    mapping(
      FirstName -> nonEmptyText,
      LastName -> nonEmptyText,
      "organization" -> optional(nonEmptyText),
      Password ->
        tuple(
          Password1 -> nonEmptyText.verifying(
            use[PasswordValidator].errorMessage,
            p => use[PasswordValidator].isValid(p)),
          Password2 -> nonEmptyText).verifying(Messages(PasswordsDoNotMatch), passwords => passwords._1 == passwords._2)) // binding
          ((firstName, lastName, organization, password) => MyRegistrationInfo(None, firstName, lastName, organization, password._1)) // unbinding
          (info => Some(info.firstName, info.lastName, info.organization, ("", ""))))

  val form = if (UsernamePasswordProvider.withUserNameSupport) formWithUsername else formWithoutUsername

  private def executeForToken(token: String, isSignUp: Boolean, f: Token => Result): Result = {
    UserService.findToken(token) match {
      case Some(t) if !t.isExpired && t.isSignUp == isSignUp => {
        f(t)
      }
      case _ =>
        Redirect(RoutesHelper.startSignUp()).flashing(Error -> InvalidLink)
    }
  }

  private def contentCollectionService = ServiceLookup.contentCollectionService
  private def orgService = ServiceLookup.orgService

  private def insertOrg(name: String) = {

    def newColl(o: Organization) = ContentCollection(ContentCollection.Default, o.id)

    for {
      org <- orgService.insert(Organization(name), None)
      collInsert <- contentCollectionService.insertCollection(newColl(org))
    } yield org
  }

  /**
   * Handles posts from the sign up page
   */
  def handleSignUp(token: String) = Action {
    implicit request =>
      executeForToken(token, true, {
        t =>
          form.bindFromRequest.fold(
            errors => {
              Logger.debug("errors " + errors)
              BadRequest(use[securesocial.controllers.TemplatesPlugin].getSignUpPage(request, securesocial.controllers.Registration.form.bind(errors.data), t.uuid))
            },
            info => {
              val id = if (UsernamePasswordProvider.withUserNameSupport) info.userName.get else t.email
              val passwordInfo = use[PasswordHasher].hash(info.password)
              val user = User(
                userName = id,
                fullName = s"${info.firstName} ${info.lastName}",
                email = t.email,
                password = passwordInfo.password,
                org = UserOrg(config.demoOrgId, Permission.Read.value),
                provider = providerId)

              def mkNewOrgOrGetDemoOrg: Validation[PlatformServiceError, ObjectId] = info.organization match {
                case Some(name) => insertOrg(name).map(_.id)
                case _ => Success(config.demoOrgId)
              }

              lazy val socialUser = {
                SocialUser(
                  IdentityId(id, providerId),
                  info.firstName,
                  info.lastName,
                  "%s %s".format(info.firstName, info.lastName),
                  Some(t.email),
                  if (UsernamePasswordProvider.enableGravatar) GravatarHelper.avatarFor(t.email) else None,
                  AuthenticationMethod.UserPassword,
                  passwordInfo = Some(passwordInfo))
              }

              val out = for {
                orgId <- mkNewOrgOrGetDemoOrg
                user <- ServiceLookup.userService.insertUser(user.copy(org = UserOrg(orgId, Permission.Write.value)))
              } yield true

              out match {
                case Success(true) => {
                  UserService.deleteToken(t.uuid)
                  if (UsernamePasswordProvider.sendWelcomeEmail) {
                    Mailer.sendWelcomeEmail(socialUser)
                  }
                  Events.fire(new SignUpEvent(socialUser))
                  Redirect(RoutesHelper.login()).flashing(RegistrationSuccess -> Messages(SignUpDone))
                }
                case Success(false) => throw new IllegalStateException("This should never happen")
                case Failure(e) => InternalServerError(
                  Json.obj(
                    "message" -> s"error occurred during registration [${e.message}]"))
              }
            })
      })
  }
}
