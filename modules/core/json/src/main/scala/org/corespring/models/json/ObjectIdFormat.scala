package org.corespring.models.json

import org.bson
import org.bson.types.ObjectId
import play.api.libs.json._

object ObjectIdFormat extends Format[ObjectId] {

  def reads(js: JsValue): JsResult[bson.types.ObjectId] = {

    try {
      val string = js.as[String]
      if (org.bson.types.ObjectId.isValid(string))
        JsSuccess(new bson.types.ObjectId(string))
      else
        JsError("Invalid object id")
    } catch {
      case e: Throwable => JsError("Invalid json")
    }
  }

  def writes(oid: bson.types.ObjectId): JsValue = JsString(oid.toString)
}
