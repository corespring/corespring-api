package common.models

import org.bson.types.ObjectId
import play.api.libs.json._


package object json {

  implicit object ObjectIdReads extends Reads[ObjectId] {
    def reads(js: JsValue): ObjectId = {
      if (ObjectId.isValid(js.as[String]))
        new ObjectId(js.as[String])
      else
        throw new RuntimeException("Invalid object id")
    }
  }

  implicit object ObjectIdWrites extends Writes[ObjectId] {
    def writes(oid: ObjectId): JsValue = JsObject(Seq("$oid" -> JsString(oid.toString)))
  }

}