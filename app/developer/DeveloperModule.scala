package developer

import developer.controllers.{ MyRegistration, Developer, AuthController }
import org.bson.types.ObjectId
import org.corespring.platform.core.controllers.auth.OAuthProvider
import org.corespring.services.UserService

case class DeveloperConfig(demoOrgId: ObjectId)

trait DeveloperModule {

  import com.softwaremill.macwire.MacwireMacros._

  def developerConfig: DeveloperConfig
  def userService: UserService
  def oauthProvider: OAuthProvider
  lazy val authController: AuthController = wire[AuthController]
  lazy val developerController: Developer = wire[Developer]
  lazy val developerControllers = Seq(authController, developerController)
}
