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
                       var licenseTypes: Seq[KeyValue] = Seq(),
                       var priorUses: Seq[KeyValue] = Seq(),
                       var demonstratedKnowledge : Seq[KeyValue] = Seq(),
                       var credentials: Seq[KeyValue] = Seq(),
                       var bloomsTaxonomy: Seq[KeyValue] = Seq(),
                       var id: ObjectId = new ObjectId()
                       ) extends Identifiable

object FieldValue extends ModelCompanion[FieldValue, ObjectId] {

  val collection = mongoCollection("fieldValues")

  val dao = new SalatDAO[FieldValue, ObjectId](collection = collection) {}

  val CurrentVersion = "0.0.1"

  val Version = "version"
  val KeySkills = "keySkills"
  val GradeLevel = "gradeLevels"
  val ReviewsPassed = "reviewsPassed"
  val ItemTypes = "itemTypes"
  val LicenseTypes = "licenseTypes"
  val PriorUses = "priorUses"
  val Credentials = "credentials"
  val BloomsTaxonomy = "bloomsTaxonomy"
  val DemonstratedKnowledge = "demonstratedKnowledge"


  def getSeqForFieldName(fieldValue:FieldValue, fieldName: String): Option[Seq[KeyValue]] = fieldName match {
    case GradeLevel => Some(fieldValue.gradeLevels)
    case ReviewsPassed => Some(fieldValue.reviewsPassed)
    case KeySkills => Some(fieldValue.keySkills)
    case ItemTypes => Some(fieldValue.itemTypes)
    case LicenseTypes => Some(fieldValue.licenseTypes)
    case PriorUses => Some(fieldValue.priorUses)
    case Credentials => Some(fieldValue.credentials)
    case BloomsTaxonomy => Some(fieldValue.bloomsTaxonomy)
    case DemonstratedKnowledge => Some(fieldValue.demonstratedKnowledge)
    case _ => None
  }

  implicit object FieldValueWrites extends Writes[FieldValue] {
    def writes(fieldValue: FieldValue) = {
      var iseq: Seq[(String, JsValue)] = Seq("id" -> JsString(fieldValue.id.toString))
      fieldValue.version.foreach(v => iseq = iseq :+ (Version -> JsString(v)))
      iseq = iseq :+ (KeySkills -> JsArray(fieldValue.keySkills.map(Json.toJson(_))))
      iseq = iseq :+ (GradeLevel -> JsArray(fieldValue.gradeLevels.map(Json.toJson(_))))
      iseq = iseq :+ (ReviewsPassed -> JsArray(fieldValue.reviewsPassed.map(Json.toJson(_))))
      iseq = iseq :+ (ItemTypes -> JsArray(fieldValue.itemTypes.map(Json.toJson(_))))
      iseq = iseq :+ (LicenseTypes -> JsArray(fieldValue.licenseTypes.map(Json.toJson(_))))
      iseq = iseq :+ (PriorUses -> JsArray(fieldValue.priorUses.map(Json.toJson(_))))
      iseq = iseq :+ (Credentials -> JsArray(fieldValue.credentials.map(Json.toJson(_))))
      iseq = iseq :+ (BloomsTaxonomy -> JsArray(fieldValue.bloomsTaxonomy.map(Json.toJson(_))))
      iseq = iseq :+ (DemonstratedKnowledge -> JsArray(fieldValue.demonstratedKnowledge.map(Json.toJson(_))))
      JsObject(iseq)
    }
  }

  val descriptions = Map(
    KeySkills -> "valid keyskills",
    GradeLevel -> "valid grade levels",
    ReviewsPassed -> "valid reviews passed",
    ItemTypes -> "valid item types (note: if you specify 'Other' you can enter freetext)",
    LicenseTypes -> "license types",
    PriorUses -> "prior uses",
    Credentials -> "credentials",
    BloomsTaxonomy -> "bloomsTaxonomy stuff",
    DemonstratedKnowledge -> "Demonstrated Knowledge"
  )
}
