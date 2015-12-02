package org.corespring.models.json.item

import org.corespring.models.item.{ Alignments, FieldValue, StringKeyValue }
import org.corespring.models.json.item.AlignmentsFormat.Keys
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class AlignmentsFormatTest extends Specification {

  val format = new AlignmentsFormat {
    override def fieldValues: FieldValue = FieldValue(
      bloomsTaxonomy = Seq(StringKeyValue("B", "B")),
      depthOfKnowledge = Seq(StringKeyValue("D", "D"))
    )
  }

  "format" should {
    "write" in {

      val alignments = Alignments(
        bloomsTaxonomy = Some("taxonomy"),
        keySkills = Seq("a", "b"),
        depthOfKnowledge = Some("depthOfKnowledge"),
        relatedCurriculum = Some("relatedCurriculum")
      )
      val json = format.writes(alignments)

      (json \ Keys.bloomsTaxonomy).asOpt[String] === alignments.bloomsTaxonomy
      (json \ Keys.keySkills).as[Seq[String]] === alignments.keySkills
      (json \ Keys.depthOfKnowledge).asOpt[String] === alignments.depthOfKnowledge
      (json \ Keys.relatedCurriculum).asOpt[String] === alignments.relatedCurriculum
    }

    "read" should {

      val json = Json.obj(
        Keys.bloomsTaxonomy -> "B",
        Keys.keySkills -> Json.arr("KS"),
        Keys.depthOfKnowledge -> "DOK",
        Keys.relatedCurriculum -> "RC"
      )

      val result = format.reads(json).get

      s"validate ${Keys.bloomsTaxonomy}" in result.bloomsTaxonomy === Some("B")
      s"not validate ${Keys.keySkills}" in result.keySkills === Seq("KS")
      s"not validate ${Keys.depthOfKnowledge}" in result.depthOfKnowledge === Some("DOK")
      s"not validate ${Keys.relatedCurriculum}" in result.relatedCurriculum === Some("RC")

    }
  }

}
