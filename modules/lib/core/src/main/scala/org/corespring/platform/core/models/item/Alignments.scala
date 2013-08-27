package org.corespring.platform.core.models.item

import play.api.libs.json._
import play.api.libs.json.JsArray
import play.api.libs.json.JsString

case class Alignments(bloomsTaxonomy: Option[String] = None,
  keySkills: Seq[String] = Seq(),
  demonstratedKnowledge: Option[String] = None,
  relatedCurriculum: Option[String] = None)

object Alignments extends ValueGetter {

  object Keys {
    val bloomsTaxonomy = "bloomsTaxonomy"
    val keySkills = "keySkills"
    val demonstratedKnowledge = "demonstratedKnowledge"
    val relatedCurriculum = "relatedCurriculum"
  }

  implicit val Writes = Json.writes[Alignments]

  implicit object Reads extends Reads[Alignments] {
    def reads(json: JsValue): JsResult[Alignments] = {

      import Keys._

      JsSuccess(new Alignments(
        demonstratedKnowledge = getValidatedValue(fieldValues.demonstratedKnowledge)(json, demonstratedKnowledge),
        bloomsTaxonomy = getValidatedValue(fieldValues.bloomsTaxonomy)(json, bloomsTaxonomy),
        keySkills = (json \ keySkills).asOpt[Seq[String]].getOrElse(Seq.empty),
        relatedCurriculum = (json \ relatedCurriculum).asOpt[String]))
    }

    private def getValidatedValue(s: Seq[StringKeyValue])(json: JsValue, key: String): Option[String] = {
      val value = (json \ key).asOpt[String]
      val out = value.filter(v => s.exists(_.key == v))
      out
    }
  }
}

