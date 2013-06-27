package developer.controllers

import play.api.mvc.{Result, Action, Controller}
import play.api.data._
import play.api.data.Forms._
import play.api.{Play, Logger}
import securesocial.core.providers.UsernamePasswordProvider
import securesocial.core._
import com.typesafe.plugin._
import Play.current
import securesocial.core.providers.utils._
import play.api.i18n.Messages
import securesocial.core.providers.Token
import scala.Some
import securesocial.controllers.Registration._
import models.User
import controllers.auth.Permission
import org.bson.types.ObjectId
import play.api.libs.json.{JsString, JsObject}
import common.config.AppConfig

object MyRegistration extends Controller {
  val Organization = "organization"

  case class MyRegistrationInfo(userName: Option[String],
                                firstName: String,
                                lastName: String,
                                organization: Option[String],
                                password: String)

  val formWithUsername = Form[MyRegistrationInfo](
    mapping(
      UserName -> nonEmptyText.verifying(Messages(UserNameAlreadyTaken), userName => {
        UserService.find(UserId(userName, providerId)).isEmpty
      }),
      FirstName -> nonEmptyText,
      LastName -> nonEmptyText,
      Organization -> optional(nonEmptyText),
      (Password ->
        tuple(
          Password1 -> nonEmptyText.verifying(use[PasswordValidator].errorMessage,
            p => use[PasswordValidator].isValid(p)
          ),
          Password2 -> nonEmptyText
        ).verifying(Messages(PasswordsDoNotMatch), passwords => passwords._1 == passwords._2)
        )
    )
      // binding
      ((userName, firstName, lastName, organization, password) => MyRegistrationInfo(Some(userName), firstName, lastName, organization, password._1))
      // unbinding
      (info => Some(info.userName.getOrElse(""), info.firstName, info.lastName, info.organization, ("", "")))
  )

  val formWithoutUsername = Form[MyRegistrationInfo](
    mapping(
      FirstName -> nonEmptyText,
      LastName -> nonEmptyText,
      Organization -> optional(nonEmptyText),
      (Password ->
        tuple(
          Password1 -> nonEmptyText.verifying(use[PasswordValidator].errorMessage,
            p => use[PasswordValidator].isValid(p)
          ),
          Password2 -> nonEmptyText
        ).verifying(Messages(PasswordsDoNotMatch), passwords => passwords._1 == passwords._2)
        )
    )
      // binding
      ((firstName, lastName, organization, password) => MyRegistrationInfo(None, firstName, lastName, organization, password._1))
      // unbinding
      (info => Some(info.firstName, info.lastName, info.organization, ("", "")))
  )

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

  /**
   * Handles posts from the sign up page
   */
  def handleSignUp(token: String) = Action {
    implicit request =>
      executeForToken(token, true, {
        t =>
          form.bindFromRequest.fold(
            errors => {
              if (Logger.isDebugEnabled) {
                Logger.debug("errors " + errors)
              }
              BadRequest(use[securesocial.controllers.TemplatesPlugin].getSignUpPage(request, securesocial.controllers.Registration.form.bind(errors.data), t.uuid))
            },
            info => {
              val id = if (UsernamePasswordProvider.withUserNameSupport) info.userName.get else t.email
              val passwordInfo = use[PasswordHasher].hash(info.password)
              val user = User(userName = id, fullName = info.firstName + " " + info.lastName, email = t.email, password = passwordInfo.password, provider = providerId)
              (info.organization match {
                case Some(orgName) => models.Organization.insert(models.Organization(orgName), None) match {
                  case Right(org) => {
                    User.insertUser(user, org.id, Permission.Write, false)
                  }
                  case Left(error) => Left(error)
                }
                case None => User.insertUser(user, AppConfig.demoOrgId, Permission.Read, false)
              }) match {
                case Right(dbuser) => {
                  val socialUser = SocialUser(
                    UserId(id, providerId),
                    info.firstName,
                    info.lastName,
                    "%s %s".format(info.firstName, info.lastName),
                    Some(t.email),
                    if (UsernamePasswordProvider.enableGravatar) GravatarHelper.avatarFor(t.email) else None,
                    AuthenticationMethod.UserPassword,
                    passwordInfo = Some(passwordInfo)
                  )
                  UserService.deleteToken(t.uuid)
                  if (UsernamePasswordProvider.sendWelcomeEmail) {
                    Mailer.sendWelcomeEmail(socialUser)
                  }
                  Redirect(RoutesHelper.login()).flashing(Success -> Messages(SignUpDone))
                }
                case Left(error) => InternalServerError(JsObject(Seq("message" -> JsString("error occurred during registration [" + error.clientOutput.getOrElse("") + "]"))))
              }
            }
          )
      })
  }
}
