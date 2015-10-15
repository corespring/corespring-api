package org.corespring.models.json.item

import org.bson.types.ObjectId
import org.corespring.models.{ Standard, Subject }
import org.corespring.models.item._
import org.corespring.models.json.JsonFormatting
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.JsValue

class ItemToSummaryWritesTest extends Specification {

  trait testEnv extends Scope {

    val detail: Option[String] = Some("normal")

    lazy val item = new Item(
      collectionId = ObjectId.get.toString,
      contributorDetails = Some(new ContributorDetails(
        author = Some("Author"),
        copyright = Some(new Copyright(
          owner = Some("Copyright Owner"))),
        credentials = Some("Test Item Writer"))),
      taskInfo = Some(new TaskInfo(
        title = Some("Title"),
        subjects = Some(new Subjects(
          primary = Some(new ObjectId("4ffb535f6bb41e469c0bf2aa")), //AP Art History
          related = Seq(new ObjectId("4ffb535f6bb41e469c0bf2ae")) //AP English Literature
          )),
        gradeLevel = Seq("GradeLevel1", "GradeLevel2"),
        itemType = Some("ItemType"))),
      standards = Seq("RL.1.5", "RI.5.8"),
      otherAlignments = Some(new Alignments(
        keySkills = Seq("KeySkill1", "KeySkill2"),
        bloomsTaxonomy = Some("BloomsTaxonomy"))),
      priorUse = Some("PriorUse"))

    val mockStandard = Standard(Some("DOT.NOTATION"))
    val mockSubject = Subject("subject", Some("category"), ObjectId.get)

    val jsonFormatting = new JsonFormatting {
      override def findStandardByDotNotation: (String) => Option[Standard] = {
        (_) => Some(mockStandard)
      }
      override def rootOrgId: ObjectId = ObjectId.get

      override def fieldValue: FieldValue = FieldValue()

      override def findSubjectById: (ObjectId) => Option[Subject] = {
        (id) => Some(mockSubject)
      }
    }

    lazy val json = new ItemToSummaryWrites(
      jsonFormatting).write(item, detail)

    def assertNormalFields = {
      (json \ "id").asOpt[String] === Some(item.id.toString)
      (json \ "author").asOpt[String] === Some("Author")
      (json \ "title").asOpt[String] === Some("Title")
      (json \ "primarySubject" \ "subject").asOpt[String] === Some("subject")
      (json \ "relatedSubject" \\ "subject").map(_.as[String]) === Seq("subject")
      (json \ "gradeLevel").as[Seq[String]] === Seq("GradeLevel1", "GradeLevel2")
      (json \ "itemType").asOpt[String] === Some("ItemType")
      val standards: Seq[JsValue] = (json \ "standards").as[Seq[JsValue]]
      (standards(0) \ "dotNotation").asOpt[String] === mockStandard.dotNotation
      (standards(1) \ "dotNotation").asOpt[String] === mockStandard.dotNotation
      (json \ "priorUse" \ "use").asOpt[String] === Some("PriorUse")
    }
  }

  "V2 - ItemTransformerToSummaryData" should {

    "when calling transform" should {

      "return normal fields" in new testEnv {
        override val detail = Some("normal")

        assertNormalFields
      }

      "return detailed fields" in new testEnv {
        override val detail = Some("detailed")

        assertNormalFields
        (json \ "copyrightOwner").asOpt[String] === Some("Copyright Owner")
        (json \ "credentials").asOpt[String] === Some("Test Item Writer")
        (json \ "keySkills").as[Seq[String]] === Seq("KeySkill1", "KeySkill2")
        (json \ "bloomsTaxonomy").asOpt[String] === Some("BloomsTaxonomy")
      }

      "return all fields" in new testEnv {
        override val detail = Some("full")

        assertNormalFields
        (json \ "copyrightOwner").asOpt[String] === Some("Copyright Owner")
        (json \ "credentials").asOpt[String] === Some("Test Item Writer")
        (json \ "keySkills").as[Seq[String]] === Seq("KeySkill1", "KeySkill2")
        (json \ "bloomsTaxonomy").asOpt[String] === Some("BloomsTaxonomy")
      }
    }
  }
}
