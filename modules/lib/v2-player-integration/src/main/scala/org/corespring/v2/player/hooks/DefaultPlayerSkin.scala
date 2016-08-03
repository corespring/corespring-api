package org.corespring.v2.player.hooks

import play.api.libs.json.{JsObject, Json}

object DefaultPlayerSkin {
  def defaultPlayerSkin: JsObject = Json.obj(
    "iconSet" -> "emoji")

}
