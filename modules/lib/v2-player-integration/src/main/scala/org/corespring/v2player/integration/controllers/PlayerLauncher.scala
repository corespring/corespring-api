package org.corespring.v2player.integration.controllers

import org.bson.types.ObjectId
import org.corespring.common.encryption.{ NullCrypto, AESCrypto }
import org.corespring.container.client.controllers.{ PlayerLauncher => ContainerPlayerLauncher }
import org.corespring.platform.core.encryption.OrgEncrypter
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.platform.core.services.UserService
import org.corespring.v2player.integration.actionBuilders.PlayerLauncherActions
import org.corespring.v2player.integration.actionBuilders.access.PlayerOptions
import play.api.mvc.{ AnyContent, Request }
import play.api.{ Configuration, Mode, Play }
import scalaz.Success
import org.slf4j.LoggerFactory
import org.corespring.platform.core.controllers.auth.SecureSocialService

class DefaultPlayerLauncherActions(
  secureSocialService: SecureSocialService,
  userService: UserService,
  val rootConfig: Configuration) extends PlayerLauncherActions(secureSocialService, userService) {

  lazy val logger = LoggerFactory.getLogger("v2player.launcher")

  override def getOrgIdAndOptions(request: Request[AnyContent]) = {
    if (encryptionEnabled(request)) {
      super.getOrgIdAndOptions(request)
    } else {
      val opts: PlayerOptions = request.getQueryString("options")
        .map(PlayerOptions.fromJson(_))
        .flatten
        .getOrElse(new PlayerOptions("*", Some("*"), true, None, Some("*")))
      val orgId: ObjectId = request.getQueryString("apiClient").map(toOrgId).flatten.getOrElse(ObjectId.get)
      Success(orgId, opts)
    }
  }

  def encryptionEnabled(r: Request[AnyContent]): Boolean = {
    val acceptsFlag = Play.current.mode == Mode.Dev || rootConfig.getBoolean("DEV_TOOLS_ENABLED").getOrElse(false)

    val enabled = if (acceptsFlag) {
      val disable = r.getQueryString("skipDecryption").map(v => true).getOrElse(false)
      !disable
    } else true
    enabled
  }

  def decrypt(request: Request[AnyContent], orgId: ObjectId, contents: String): Option[String] = for {
    encrypter <- Some(if (encryptionEnabled(request)) AESCrypto else NullCrypto)
    orgEncrypter <- Some(new OrgEncrypter(orgId, encrypter))
    out <- orgEncrypter.decrypt(contents)
  } yield out

  def toOrgId(apiClientId: String): Option[ObjectId] = {
    logger.debug(s"[toOrgId] find org for apiClient: $apiClientId")
    val client = ApiClient.findByKey(apiClientId)

    if (client.isEmpty) {
      logger.warn(s"[toOrgId] can't find org for $apiClientId")
    }
    client.map(_.orgId)
  }

}
