package org.corespring.platform.core

import org.corespring.platform.core.controllers.auth.OAuthProvider
import org.corespring.services.{ UserService, OrganizationService }
import org.corespring.services.auth.{ AccessTokenService, ApiClientService }

trait LegacyModule {

  import com.softwaremill.macwire.MacwireMacros._

  def apiClientService: ApiClientService
  def orgService: OrganizationService
  def tokenService: AccessTokenService
  def userService: UserService

  lazy val oauthProvider = wire[OAuthProvider]
}
