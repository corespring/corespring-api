package models.item

import play.api.libs.json._
import play.api.libs.json.JsString
import com.novus.salat.dao._
import se.radley.plugin.salat._
import com.mongodb.casbah.Imports._
import models.mongoContext._
import play.api.Play.current

case class ListKeyValue(key: String, value: Seq[String])
case class StringKeyValue(key:String, value: String)

object ListKeyValue{
  implicit val Writes = Json.writes[ListKeyValue]
}

object StringKeyValue{
  implicit val Writes = Json.writes[StringKeyValue]
}

case class FieldValue(
                       var version: Option[String] = None,
                       var gradeLevels: Seq[StringKeyValue] = Seq(),
                       var reviewsPassed: Seq[StringKeyValue] = Seq(),
                       var keySkills: Seq[ListKeyValue] = Seq(),
                       var itemTypes: Seq[ListKeyValue] = Seq(),
                       var licenseTypes: Seq[StringKeyValue] = Seq(),
                       var priorUses: Seq[StringKeyValue] = Seq(),
                       var demonstratedKnowledge : Seq[StringKeyValue] = Seq(),
                       var credentials: Seq[StringKeyValue] = Seq(),
                       var bloomsTaxonomy: Seq[StringKeyValue] = Seq(),
                       var id: ObjectId = new ObjectId()
                       )

object FieldValue extends ModelCompanion[FieldValue, ObjectId] {

  import play.api.Play.current

  lazy val current = FieldValue.findOne(MongoDBObject(FieldValue.Version -> CurrentVersion)).getOrElse(throw new RuntimeException("could not find field values doc with specified version"))

  val collection = mongoCollection("fieldValues")(play.api.Play.current)

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


  def getSeqForFieldName(fieldValue:FieldValue, fieldName: String): Option[JsValue] = {

    def o[A](v:A)(implicit writes : Writes[A]) : Option[JsValue] = Some(play.api.libs.json.Json.toJson(v))

    fieldName match {
      case GradeLevel => o(fieldValue.gradeLevels)
      case ReviewsPassed => o(fieldValue.reviewsPassed)
      case KeySkills => o(fieldValue.keySkills)
      case ItemTypes => o(fieldValue.itemTypes)
      case LicenseTypes => o(fieldValue.licenseTypes)
      case PriorUses => o(fieldValue.priorUses)
      case Credentials => o(fieldValue.credentials)
      case BloomsTaxonomy => o(fieldValue.bloomsTaxonomy)
      case DemonstratedKnowledge => o(fieldValue.demonstratedKnowledge)
      case _ => None
    }
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
