package org.corespring.v2player.integration.actionBuilders.access

import org.corespring.player.accessControl.cookies.BasePlayerCookieReader

trait V2PlayerCookieReader extends BasePlayerCookieReader[Mode.Mode, PlayerOptions]{
  def toMode(s: String): Mode.Mode = Mode.withName(s)

  def toOptions(json: String): PlayerOptions = PlayerOptions.ANYTHING
}


