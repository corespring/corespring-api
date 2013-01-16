package models.item

import play.api.libs.json._
import models.KeyValue
import models.FieldValue
import controllers.JsonValidationException
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import scala.Some

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

  implicit object Writes extends Writes[Alignments] {
    def writes(a: Alignments): JsValue = {
      import Keys._

      JsObject(Seq(
        a.bloomsTaxonomy.map((bloomsTaxonomy -> JsString(_))),
        Some((keySkills -> JsArray(a.keySkills.map(JsString(_))))),
        a.demonstratedKnowledge.map((demonstratedKnowledge -> JsString(_))),
        a.relatedCurriculum.map((relatedCurriculum -> JsString(_)))
      ).flatten)
    }
  }

  implicit object Reads extends Reads[Alignments] {
    def reads(json: JsValue): Alignments = {

      import Keys._

      new Alignments(
        demonstratedKnowledge = getValidatedValue(fieldValues.demonstratedKnowledge)(json, demonstratedKnowledge),
        bloomsTaxonomy = getValidatedValue(fieldValues.bloomsTaxonomy)(json, bloomsTaxonomy),
        keySkills = (json \ keySkills).asOpt[Seq[String]].getOrElse(Seq.empty),
        relatedCurriculum = (json \ relatedCurriculum).asOpt[String]
      )
    }

    private def getValidatedValue(s: Seq[KeyValue])(json: JsValue, key: String): Option[String] = {
      val value = (json \ key).asOpt[String]
      val out = value.filter(v => s.exists(_.key == v))
      out
    }
  }
}



