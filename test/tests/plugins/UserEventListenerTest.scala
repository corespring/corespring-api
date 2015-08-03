package tests.plugins

import org.corespring.models.User
import org.corespring.test.{ TestModelHelpers, BaseTest }
import org.joda.time.DateTime
import play.api.mvc.Session
import play.api.test.FakeRequest
import plugins.UserEventListener
import securesocial.core.AuthenticationMethod.UserPassword
import securesocial.core._
import org.corespring.models.auth.Permission

class UserEventListenerTest extends BaseTest with TestModelHelpers {

  case class MockIdentity(identityId: IdentityId, firstName: String = "", lastName: String = "", fullName: String = "", email: Option[String] = None,
    avatarUrl: Option[String] = None, authMethod: AuthenticationMethod = UserPassword,
    oAuth1Info: Option[OAuth1Info] = None,
    oAuth2Info: Option[OAuth2Info] = None,
    passwordInfo: Option[PasswordInfo] = None) extends Identity

  def identity(userId: IdentityId) = MockIdentity(userId)

  val userEventListener = new UserEventListener(null)
  def userId(user: User) = IdentityId(user.userName, "userpass")

  def userHasDate(user: User, fn: User => Option[DateTime]) = {
    val hasDate: Boolean = User.getUser(userId(user)) match {
      case Some(user) => !fn(user).isEmpty
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
