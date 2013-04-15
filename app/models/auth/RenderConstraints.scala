package models.auth

import org.bson.types.ObjectId
import play.api.libs.json._

case class RenderConstraints(itemIds:Seq[ObjectId])
object RenderConstraints{
  implicit object RCReads extends Reads[RenderConstraints]{
    def reads(json:JsValue):RenderConstraints = {
      RenderConstraints(
        (json \ "itemIds").as[Seq[String]].map(new ObjectId(_))
      )
    }
  }
  implicit object RCWrites extends Writes[RenderConstraints]{
    def writes(rc:RenderConstraints):JsValue = {
      JsObject(Seq(
        "itemIds" -> JsArray(rc.itemIds.map(id => JsString(id.toString)))
      ))
    }
  }
}
