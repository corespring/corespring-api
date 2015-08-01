package org.corespring.models.item

case class Alignments(bloomsTaxonomy: Option[String] = None,
  keySkills: Seq[String] = Seq(),
  depthOfKnowledge: Option[String] = None,
  relatedCurriculum: Option[String] = None)

import scala.language.experimental.macros

object Alignments {

  object Keys {
    val bloomsTaxonomy = "bloomsTaxonomy"
    val keySkills = "keySkills"
    val depthOfKnowledge = "depthOfKnowledge"
    val relatedCurriculum = "relatedCurriculum"
  }
}

