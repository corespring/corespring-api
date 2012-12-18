package developer.controllers

import securesocial.controllers.TemplatesPlugin
import securesocial.core.{SecuredRequest, SocialUser}
import play.api.mvc.{Request, RequestHeader}
import play.api.data.Form
import securesocial.controllers.Registration.RegistrationInfo
import play.api.templates.Html
import play.api.Application

/**
 * A plugin to customise the views used by SecureSocial.
 * We use this instead of the module's DefaultTemplatesView
 */
class CoreSpringViews(application: Application) extends TemplatesPlugin {
  /**
   * Returns the html for the login page
   * @param request
   * @tparam A
   * @return
   */
  override def getLoginPage[A](implicit request: Request[A], form: Form[(String, String)],
                               msg: Option[String] = None): Html = {

    developer.views.html.login(form, msg)
  }

  /**
   * Returns the html for the signup page
   *
   * @param request
   * @tparam A
   * @return
   */
  def getSignUpPage[A](implicit request: Request[A], form: Form[RegistrationInfo], token: String) =
    developer.views.html.signup(form, token)

  /**
   * Returns the html for the start signup page
   *
   * @param request
   * @tparam A
   * @return
   */
  def getStartSignUpPage[A](implicit request: Request[A], form: Form[String]) =
    developer.views.html.startSignup(form)

  /**
   * Returns the html for the reset password page
   *
   * @param request
   * @tparam A
   * @return
   */
  def getResetPasswordPage[A](implicit request: Request[A], form: Form[(String, String)], token: String) =
    developer.views.html.resetPasswordPage(form, token)

  /**
   * Returns the html for the start reset page
   *
   * @param request
   * @tparam A
   * @return
   */
  def getStartResetPasswordPage[A](implicit request: Request[A], form: Form[String]) =
    developer.views.html.startResetPassword(form)

  /**
   * Returns the email sent when a user starts the sign up process
   *
   * @param token the token used to identify the request
   * @param request the current http request
   * @return a String with the html code for the email
   */
  def getSignUpEmail(token: String)(implicit request: RequestHeader) = {
    developer.views.html.mails.signUpEmail(token).body
  }

  /**
   * Returns the email sent when the user is already registered
   *
   * @param user the user
   * @param request the current request
   * @return a String with the html code for the email
   */
  def getAlreadyRegisteredEmail(user: SocialUser)(implicit request: RequestHeader) = {
    developer.views.html.mails.alreadyRegisteredEmail(user).body
  }

  /**
   * Returns the welcome email sent when the user finished the sign up process
   *
   * @param user the user
   * @param request the current request
   * @return a String with the html code for the email
   */
  def getWelcomeEmail(user: SocialUser)(implicit request: RequestHeader) = {
    developer.views.html.mails.welcomeEmail(user).body
  }

  /**
   * Returns the email sent when a user tries to reset the password but there is no account for
   * that email address in the system
   *
   * @param request the current request
   * @return a String with the html code for the email
   */
  def getUnknownEmailNotice()(implicit request: RequestHeader) = {
    developer.views.html.mails.unknownEmailNotice(request).body
  }

  /**
   * Returns the email sent to the user to reset the password
   *
   * @param user the user
   * @param token the token used to identify the request
   * @param request the current http request
   * @return a String with the html code for the email
   */
  def getSendPasswordResetEmail(user: SocialUser, token: String)(implicit request: RequestHeader) = {
    developer.views.html.mails.passwordResetEmail(user, token).body
  }

  /**
   * Returns the email sent as a confirmation of a password change
   *
   * @param user the user
   * @param request the current http request
   * @return a String with the html code for the email
   */
  def getPasswordChangedNotice(user: SocialUser)(implicit request: RequestHeader) = null

  def getPasswordChangePage[A](implicit request: SecuredRequest[A], form: Form[securesocial.controllers.PasswordChange.ChangeInfo]): Html = null

  def getPasswordChangedNoticeEmail(user: securesocial.core.SocialUser)(implicit request: play.api.mvc.RequestHeader): scala.Predef.String = {
    developer.views.html.mails.passwordChangedNotice(user).body
  }
}
