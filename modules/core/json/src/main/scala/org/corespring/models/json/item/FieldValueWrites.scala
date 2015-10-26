package org.corespring.models.json.item

import org.corespring.models.item.{ ListKeyValue, StringKeyValue, FieldValue }
import org.corespring.models.item.FieldValue._
import play.api.libs.json._

object FieldValueWrites extends Writes[FieldValue] {

  implicit val lkv = Json.writes[ListKeyValue]

  implicit val skv = Json.writes[StringKeyValue]

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
    iseq = iseq :+ (MediaType -> JsArray(fieldValue.mediaType.map(Json.toJson(_))))
    iseq = iseq :+ (BloomsTaxonomy -> JsArray(fieldValue.bloomsTaxonomy.map(Json.toJson(_))))
    iseq = iseq :+ (DepthOfKnowledge -> JsArray(fieldValue.depthOfKnowledge.map(Json.toJson(_))))
    JsObject(iseq)
  }
}

