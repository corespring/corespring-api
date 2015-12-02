package org.corespring.models.json.item

import org.corespring.models.item.{ StringKeyValue, Alignments => Model }
import org.corespring.models.json.{ ValueGetter, JsonUtil }
import play.api.libs.json._

object AlignmentsFormat {
  object Keys {
    val bloomsTaxonomy = "bloomsTaxonomy"
    val keySkills = "keySkills"
    val depthOfKnowledge = "depthOfKnowledge"
    val relatedCurriculum = "relatedCurriculum"
  }
}

trait AlignmentsFormat extends ValueGetter with JsonUtil with Format[Model] {

  import AlignmentsFormat.Keys._
  object Values {
    val none = "None"
  }

  def writes(alignments: Model): JsValue = {
    partialObj(
      bloomsTaxonomy -> alignments.bloomsTaxonomy.map(JsString(_)),
      depthOfKnowledge -> alignments.depthOfKnowledge.map(JsString(_)),
      keySkills -> (alignments.keySkills.map(JsString(_)) match {
        case skills: Seq[JsString] if (skills.isEmpty) => None
        case skills: Seq[JsString] => Some(JsArray(skills))
      }),
      relatedCurriculum -> alignments.relatedCurriculum.map(JsString(_))
    )
  }

  def reads(json: JsValue): JsResult[Model] = {

    JsSuccess(new Model(
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
