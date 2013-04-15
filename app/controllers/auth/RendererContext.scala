package controllers.auth

import play.api.libs.json._
import org.bson.types.ObjectId
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import scala.Some

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
case class RenderConstraints(itemId:String)
object RenderConstraints{
  val ALL_ITEMS = "*"
  implicit object RCReads extends Reads[RenderConstraints]{
    def reads(json:JsValue):RenderConstraints = {
      RenderConstraints(
        (json \ "itemId").as[String]
      )
    }
  }
  implicit object RCWrites extends Writes[RenderConstraints]{
    def writes(rc:RenderConstraints):JsValue = {
      JsObject(Seq(
        "itemId" -> JsString(rc.itemId)
      ))
    }
  }
}
