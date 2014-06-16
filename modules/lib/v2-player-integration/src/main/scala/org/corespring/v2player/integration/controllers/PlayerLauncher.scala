package org.corespring.v2player.integration.controllers

import org.corespring.platform.core.controllers.auth.SecureSocialService
import org.corespring.platform.core.services.UserService
import org.corespring.v2player.integration.actionBuilders.PlayerLauncherActions
import play.api.Configuration

abstract class DefaultPlayerLauncherActions(
  secureSocialService: SecureSocialService,
  userService: UserService,
  val rootConfig: Configuration) extends PlayerLauncherActions(secureSocialService, userService) {

}
