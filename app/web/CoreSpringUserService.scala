package web

import _root_.controllers.auth.Permission
import _root_.controllers.Log
import _root_.models.{UserOrg, User}
import securesocial.core._
import play.api.Application
import org.bson.types.ObjectId
import securesocial.core.UserId
import securesocial.core.SocialUser
import scala.Some

/**
 * An implementation of the UserService
 */
class CoreSpringUserService(application: Application) extends UserServicePlugin(application) {
  def find(id: UserId): Option[SocialUser] = {
    // id.id has the username
    Log.i("looking for %s(%s)".format(id.id, id.providerId))
    User.getUser(id.id).map( u => {
      Log.i("found user = " + u.userName)
      SocialUser(id, u.fullName, Some(u.email), None, AuthenticationMethod.UserPassword, isEmailVerified = true, passwordInfo = Some(PasswordInfo(u.password)) )
    })
  }

  def save(user: SocialUser) {
   if ( User.getUser(user.id.id).isEmpty ) {
      val corespringUser = User(user.id.id, user.displayName, user.email.get, Seq(), user.passwordInfo.get.password, new ObjectId())
      // hardcode this org id for now?
      val corespringId = new ObjectId("502404dd0364dc35bb39339a")
      User.insertUser(corespringUser, corespringId, Permission.All, checkOrgId = false)
    }
  }

  def createActivation(user: SocialUser): String = {
    // Just a dummy implementation since we're not allowing user registrations for now
    ""
  }

  def activateAccount(uuid: String): Boolean = {
    // Just a dummy implementation since we're not allowing user registrations for now
    false
  }
}
