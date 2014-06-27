package org.corespring.platform.core.models.item

import play.api.libs.json._
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import org.corespring.platform.core.models.JsonUtil

case class Alignments(bloomsTaxonomy: Option[String] = None,
  keySkills: Seq[String] = Seq(),
  demonstratedKnowledge: Option[String] = None,
  depthOfKnowledge: Option[String] = None,
  relatedCurriculum: Option[String] = None)

object Alignments extends ValueGetter with JsonUtil {

  object Keys {
    val bloomsTaxonomy = "bloomsTaxonomy"
    val keySkills = "keySkills"
    val demonstratedKnowledge = "demonstratedKnowledge"
    val depthOfKnowledge = "depthOfKnowledge"
    val relatedCurriculum = "relatedCurriculum"
  }

  object Values {
    val none = "None"
  }

  implicit object Writes extends Writes[Alignments]{

    def writes(alignments: Alignments): JsValue = {
      import Keys._
      partialObj(
        demonstratedKnowledge -> (alignments.demonstratedKnowledge match {
          case Some(demonstrated) if demonstrated != Values.none => Some(demonstrated)
          case _ => None
        }).map(JsString(_)),
        bloomsTaxonomy -> alignments.bloomsTaxonomy.map(JsString(_)),
        depthOfKnowledge -> alignments.depthOfKnowledge.map(JsString(_)),
        keySkills -> (alignments.keySkills.map(JsString(_)) match {
          case skills: Seq[JsString] if (skills.isEmpty) => None
          case skills: Seq[JsString] => Some(JsArray(skills))
        }),
        relatedCurriculum -> alignments.relatedCurriculum.map(JsString(_))
      )
    }

  }

  implicit object Reads extends Reads[Alignments] {
    def reads(json: JsValue): JsResult[Alignments] = {

      import Keys._

      JsSuccess(new Alignments(
        demonstratedKnowledge = getValidatedValue(fieldValues.demonstratedKnowledge)(json, demonstratedKnowledge),
        bloomsTaxonomy = getValidatedValue(fieldValues.bloomsTaxonomy)(json, bloomsTaxonomy),
        keySkills = (json \ keySkills).asOpt[Seq[String]].getOrElse(Seq.empty),
        depthOfKnowledge = (json \ depthOfKnowledge).asOpt[String],
        relatedCurriculum = (json \ relatedCurriculum).asOpt[String]))
    }

    private def getValidatedValue(s: Seq[StringKeyValue])(json: JsValue, key: String): Option[String] = {
      val value = (json \ key).asOpt[String]
      val out = value.filter(v => s.exists(_.key == v))
      out
    }
  }
}

