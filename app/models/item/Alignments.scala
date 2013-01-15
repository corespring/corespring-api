package models.item

import play.api.libs.json.{JsArray, JsString, JsValue, JsNull}
import models.KeyValue
import models.FieldValue
import controllers.JsonValidationException

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

  def json(a: Alignments): Seq[(String, JsValue)] = {
    import Keys._

    Seq(
      a.bloomsTaxonomy.map((bloomsTaxonomy -> JsString(_))),
      Some((keySkills -> JsArray(a.keySkills.map(JsString(_))))),
      a.demonstratedKnowledge.map((demonstratedKnowledge -> JsString(_))),
      a.relatedCurriculum.map((relatedCurriculum -> JsString(_)))
    ).flatten
  }

  def getValidatedValue(s: Seq[KeyValue])(json: JsValue, key: String): Option[String] = {
    val value = (json \ key).asOpt[String]
    val out = value.filter(v => s.exists(_.key == v))
    out
  }

  def obj(js: JsValue): Option[Alignments] = {
    import Keys._

    Some(
      new Alignments(
        demonstratedKnowledge = getValidatedValue(fieldValues.demonstratedKnowledge)(js, demonstratedKnowledge),
        bloomsTaxonomy = getValidatedValue(fieldValues.bloomsTaxonomy)(js, bloomsTaxonomy),
        keySkills = (js \ keySkills).asOpt[Seq[String]].getOrElse(Seq.empty),
        relatedCurriculum = (js \ relatedCurriculum).asOpt[String]
      )
    )

  }

}



