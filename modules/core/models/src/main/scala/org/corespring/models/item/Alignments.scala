package org.corespring.models.item

case class Alignments(bloomsTaxonomy: Option[String] = None,
  keySkills: Seq[String] = Seq(),
  depthOfKnowledge: Option[String] = None,
  relatedCurriculum: Option[String] = None)

