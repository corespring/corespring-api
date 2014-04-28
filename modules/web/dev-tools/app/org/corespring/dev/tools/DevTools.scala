package org.corespring.dev.tools

import play.api.{ Mode, Play }

object DevTools {

  val key = "DEV_TOOLS_ENABLED"
  def enabled = Play.current.mode == Mode.Dev || Play.current.configuration.getBoolean(key).getOrElse(false)
}
