package developer

import developer.controllers.AuthController
import org.corespring.platform.core.controllers.auth.OAuthProvider
import org.corespring.services.UserService

trait DeveloperModule {

  import com.softwaremill.macwire.MacwireMacros._

  def userService: UserService
  def oauthProvider: OAuthProvider
  def authController: AuthController = wire[AuthController]

  lazy val developerControllers = Seq(authController)
}
