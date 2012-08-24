package models

import play.api.Play.current
import se.radley.plugin.salat._
import play.api.libs.json._
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import org.bson.types.ObjectId
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

import play.api.libs.json._
import play.api.libs.json.JsString
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import com.novus.salat.dao._
import se.radley.plugin.salat._
import mongoContext._

case class KeyValue(key: String, value: Any)

object KeyValue {

  implicit object KeyValueWrites extends Writes[KeyValue] {
    def writes(keyValue: KeyValue) = {
      JsObject(Seq[(String, JsValue)]("key" -> JsString(keyValue.key), "value" -> JsString(keyValue.value.toString)))
    }
  }

}


case class FieldValue(
                       var version: Option[String] = None,
                       var gradeLevels: Seq[KeyValue] = Seq(),
                       var reviewsPassed: Seq[KeyValue] = Seq(),
                       var keySkills: Seq[KeyValue] = Seq(),
                       var itemTypes: Seq[KeyValue] = Seq(),
                       var id: ObjectId = new ObjectId()
                       )

object FieldValue extends ModelCompanion[FieldValue, ObjectId] {

  val collection = mongoCollection("fieldValues")

  val dao = new SalatDAO[FieldValue, ObjectId](collection = collection) {}

  val Version = "version"
  val KeySkills = "keySkills"
  val GradeLevel = "gradeLevel"
  val ReviewsPassed = "reviewsPassed"
  val ItemTypes = "itemTypes"

  implicit object FieldValueWrites extends Writes[FieldValue] {
    def writes(fieldValue: FieldValue) = {
      var iseq: Seq[(String, JsValue)] = Seq("id" -> JsString(fieldValue.id.toString))
      fieldValue.version.foreach(v => iseq = iseq :+ (Version -> JsString(v)))
      iseq = iseq :+ (KeySkills -> JsArray(fieldValue.keySkills.map(Json.toJson(_))))
      iseq = iseq :+ (GradeLevel -> JsArray(fieldValue.gradeLevels.map(Json.toJson(_))))
      iseq = iseq :+ (ReviewsPassed -> JsArray(fieldValue.reviewsPassed.map(Json.toJson(_))))
      iseq = iseq :+ (ItemTypes -> JsArray(fieldValue.itemTypes.map(Json.toJson(_))))
      JsObject(iseq)
    }
  }

  val descriptions = Map(
    KeySkills -> "valid keyskills",
    GradeLevel -> "valid grade levels",
    ReviewsPassed -> "valid reviews passed",
    ItemTypes -> "valid item types"
  )
}
