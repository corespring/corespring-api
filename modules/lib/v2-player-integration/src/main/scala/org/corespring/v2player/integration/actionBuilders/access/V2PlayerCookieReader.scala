package org.corespring.v2player.integration.actionBuilders.access

import org.corespring.player.accessControl.cookies.{CookieKeys, BasePlayerCookieWriter, BasePlayerCookieReader}

object V2PlayerCookieKeys extends CookieKeys {
  val activeMode: String = "v2player.activeMode"
  val orgId: String = "v2Player.orgId"
  val renderOptions: String = "v2Player.renderOptions"
}

trait V2PlayerCookieReader extends BasePlayerCookieReader[Mode.Mode, PlayerOptions] {
  def toMode(s: String): Mode.Mode = Mode.withName(s)

  def toOptions(json: String): PlayerOptions = PlayerOptions.ANYTHING

  def keys = V2PlayerCookieKeys
}

trait V2PlayerCookieWriter extends BasePlayerCookieWriter[Mode.Mode, PlayerOptions]{
  def keys = V2PlayerCookieKeys
}



