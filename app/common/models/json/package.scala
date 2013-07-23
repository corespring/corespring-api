package common.models

import org.bson.types.ObjectId
import play.api.libs.json._


package object json {

  implicit object ObjectIdReads extends Reads[ObjectId] {
    def reads(js: JsValue): JsResult[ObjectId] = {
      if (ObjectId.isValid(js.as[String]))
        JsSuccess(new ObjectId(js.as[String]))
      else
        JsError("Invalid object id")
    }
  }

  implicit object ObjectIdWrites extends Writes[ObjectId] {
    def writes(oid: ObjectId): JsValue = JsString(oid.toString)
  }

}