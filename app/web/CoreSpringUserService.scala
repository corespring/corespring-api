package web

import _root_.controllers.auth.Permission
import _root_.controllers.Log
import _root_.models.{UserOrg, User}
import securesocial.core._
import play.api.Application
import org.bson.types.ObjectId
import providers.Token
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
      SocialUser(id, "", "", u.fullName, Some(u.email), None, AuthenticationMethod.UserPassword, passwordInfo = Some(PasswordInfo(u.password)) )
    })
  }

  def save(user: SocialUser) {
   if ( User.getUser(user.id.id).isEmpty ) {
      val corespringUser = User(user.id.id, user.fullName, user.email.get, Seq(), user.passwordInfo.get.password, new ObjectId())
      // hardcode this org id for now?
      val corespringId = new ObjectId("502404dd0364dc35bb39339a")
      User.insertUser(corespringUser, corespringId, Permission.All, checkOrgId = false)
    }
  }

  /**
   * Finds a Social user by email and provider id.
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation.
   *
   * @param email - the user email
   * @param providerId - the provider id
   * @return
   */
  def findByEmail(email: String, providerId: String) = None

  /**
   * Saves a token.  This is needed for users that
   * are creating an account in the system instead of using one in a 3rd party system.
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param token The token to save
   * @return A string with a uuid that will be embedded in the welcome email.
   */
  def save(token: Token) {}

  /**
   * Finds a token
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param token the token id
   * @return
   */
  def findToken(token: String) = None

  /**
   * Deletes a token
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param uuid the token id
   */
  def deleteToken(uuid: String) {}

  /**
   * Deletes all expired tokens
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   */
  def deleteExpiredTokens() {}
}
