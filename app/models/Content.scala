package models

import org.bson.types.ObjectId
import play.api.libs.json.JsValue
import se.radley.plugin.salat._
import play.api.Play.current

trait Content {
  var id: ObjectId;
  val contentType: String;
  var collId: ObjectId;

  def toJson(): JsValue;
}

object Content {
  val collId: String = "collId"
  val contentType: String = "contentType"

  val collection = mongoCollection("content")
}

object ContentType {
  val item = "item"
  val assessment = "assessment"

  def isContentType(s: String): Boolean = s == item || s == assessment
}
