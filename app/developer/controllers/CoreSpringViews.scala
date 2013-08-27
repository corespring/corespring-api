package developer.controllers

import play.api.Application
import play.api.data.Form
import play.api.mvc.{ Request, RequestHeader }
import play.api.templates.{ Txt, Html }
import securesocial.controllers.Registration.RegistrationInfo
import securesocial.controllers.TemplatesPlugin
import securesocial.core.{ Identity, SecuredRequest }

class CoreSpringViews(application: Application) extends TemplatesPlugin {

  override def getLoginPage[A](implicit request: Request[A], form: Form[(String, String)], msg: Option[String] = None): Html = developer.views.html.login(form, msg)

  override def getSignUpPage[A](implicit request: Request[A], form: Form[RegistrationInfo], token: String) = developer.views.html.signup(form, token)

  override def getStartSignUpPage[A](implicit request: Request[A], form: Form[String]) = developer.views.html.startSignup(form)

  override def getResetPasswordPage[A](implicit request: Request[A], form: Form[(String, String)], token: String) = developer.views.html.resetPasswordPage(form, token)

  override def getStartResetPasswordPage[A](implicit request: Request[A], form: Form[String]) = developer.views.html.startResetPassword(form)

  override def getSignUpEmail(token: String)(implicit request: RequestHeader) = html(developer.views.html.mails.signUpEmail(token))

  override def getAlreadyRegisteredEmail(user: Identity)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = html(developer.views.html.mails.alreadyRegisteredEmail(user))

  override def getWelcomeEmail(user: Identity)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = html(developer.views.html.mails.welcomeEmail(user))

  override def getUnknownEmailNotice()(implicit request: RequestHeader) = html(developer.views.html.mails.unknownEmailNotice(request))

  override def getSendPasswordResetEmail(user: Identity, token: String)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = html(developer.views.html.mails.passwordResetEmail(user, token))

  override def getPasswordChangePage[A](implicit request: SecuredRequest[A], form: Form[securesocial.controllers.PasswordChange.ChangeInfo]): Html = null

  override def getPasswordChangedNoticeEmail(user: Identity)(implicit request: play.api.mvc.RequestHeader): (Option[Txt], Option[Html]) = html(developer.views.html.mails.passwordChangedNotice(user))

  override def getNotAuthorizedPage[A](implicit request: Request[A]): Html = developer.views.html.notAuthorised()

  private def html(html: Html): (Option[Txt], Option[Html]) = (None, Some(html))

}
