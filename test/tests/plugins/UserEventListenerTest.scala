package tests.plugins

import org.specs2.mutable.Specification
import tests.helpers.TestModelHelpers
import securesocial.core._
import securesocial.core.PasswordInfo
import securesocial.core.LoginEvent
import securesocial.core.OAuth2Info
import securesocial.core.AuthenticationMethod.UserPassword
import models.User
import plugins.UserEventListener
import play.api.mvc.Session
import play.api.test.FakeRequest
import tests.BaseTest
import controllers.auth.Permission
import org.joda.time.DateTime

class UserEventListenerTest extends BaseTest with TestModelHelpers {

  case class MockIdentity(id: UserId, firstName: String = "", lastName: String = "", fullName: String = "", email: Option[String] = None,
                     avatarUrl: Option[String] = None, authMethod: AuthenticationMethod = UserPassword,
                     oAuth1Info: Option[OAuth1Info] = None,
                     oAuth2Info: Option[OAuth2Info] = None,
                     passwordInfo: Option[PasswordInfo] = None) extends Identity

  def identity(userId: UserId) = MockIdentity(id = userId)

  val userEventListener = new UserEventListener(null)
  def userId(user: User) = UserId(user.userName, "userpass")

  def userHasDate(user: User, fn: User => Option[DateTime]) = {
    val hasDate: Boolean = User.getUser(userId(user)) match {
      case Some(user) => {
        fn(user) match {
          case Some(date) => true
          case None => false
        }
      }
      case None => false
    }
    hasDate === true
  }

  "UserEventListener" should {

    "update lastLoginDate on LoginEvent" in new TestOPlenty(Permission.Read) {
      val loginEvent = LoginEvent(identity(userId(user)))

      user.lastLoginDate === None
      userEventListener.onEvent(loginEvent, FakeRequest(), Session(Map.empty[String, String]))
      userHasDate(user, _.lastLoginDate)
    }

    "update registrationDate on SignUpEvent" in new TestOPlenty(Permission.Read) {
      val signupEvent = SignUpEvent(identity(userId(user)))

      user.registrationDate === None
      userEventListener.onEvent(signupEvent, FakeRequest(), Session(Map.empty[String, String]))
      userHasDate(user, _.registrationDate)
    }

  }

}
