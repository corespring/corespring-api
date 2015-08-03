package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.item._
import org.corespring.test.PlaySingleton
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.{ Scope, BeforeAfter }
import play.api.libs.json.JsValue

class ItemToSummaryDataTest extends Specification with Mockito {

  /**
   * We should not need to run the app for a unit test.
   * However the way the app is tied up (global Objects)
   * we need to boot a play application.
   */
  PlaySingleton.start()

  trait testEnv extends Scope {

    val detail: Option[String] = Some("normal")

    lazy val item = new Item(
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

    lazy val itemTransformer = new ItemToSummaryData {}

    lazy val json = itemTransformer.toSummaryData(item, detail)

    def assertNormalFields = {
      (json \ "id").asOpt[String] === Some(item.id.toString)
      (json \ "author").asOpt[String] === Some("Author")
      (json \ "title").asOpt[String] === Some("Title")
      (json \ "primarySubject" \ "subject").asOpt[String] === Some("AP Art History")
      (json \ "relatedSubject" \\ "subject").map(_.as[String]) === Seq("AP English Literature")
      (json \ "gradeLevel").as[Seq[String]] === Seq("GradeLevel1", "GradeLevel2")
      (json \ "itemType").asOpt[String] === Some("ItemType")
      val standards: Seq[JsValue] = (json \ "standards").as[Seq[JsValue]]
      (standards(0) \ "dotNotation").asOpt[String] === Some("RL.1.5")
      (standards(1) \ "dotNotation").asOpt[String] === Some("RI.5.8")
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
