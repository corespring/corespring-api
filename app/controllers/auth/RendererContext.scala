package controllers.auth

import models.auth.RenderConstraints
import play.api.libs.json.Json

case class RendererContext(clientId: String, rc:RenderConstraints)
object RendererContext{
  val keyDelimeter = "-"
  def parserRendererContext(key:String):Option[RendererContext] = {
    val parts = key.split(keyDelimeter)
    if (parts.length == 2){
      val rc = Json.fromJson[RenderConstraints](Json.parse(parts(1)))
      Some(RendererContext(parts(0),rc))
    } else None
  }
}
