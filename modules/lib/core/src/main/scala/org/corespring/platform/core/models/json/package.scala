package org.corespring.platform.core.models

import play.api.libs.json._
import org.bson.types.ObjectId
import play.api.libs.json.JsSuccess

package object json {
  case class JsonValidationException(field: String) extends RuntimeException("invalid value for: " + field)

  implicit object ObjectIdReads extends Reads[ObjectId] {
    def reads(js: JsValue): JsResult[ObjectId] = {

      try {
        val string = js.as[String]
        if (ObjectId.isValid(string))
          JsSuccess(new ObjectId(string))
        else
          JsError("Invalid object id")
      } catch {
        case e: Throwable => JsError("Invalid json")
      }
    }
  }

  implicit object ObjectIdWrites extends Writes[ObjectId] {
    def writes(oid: ObjectId): JsValue = JsString(oid.toString)
  }
}
