package org.corespring.platform.core.models.item

import org.bson.types.ObjectId
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsNumber

case class Version(root:ObjectId, rev:Int, current:Boolean)
object Version{
  val rev = "rev"
  val root = "root"
  val current = "current"

  implicit object VersionWrites extends Writes[Version]{
    def writes(ver:Version):JsValue = {
      JsObject(Seq(
        rev -> JsNumber(ver.rev),
        root -> JsString(ver.root.toString),
        current -> JsBoolean(ver.current)
      ))
    }
  }
}
