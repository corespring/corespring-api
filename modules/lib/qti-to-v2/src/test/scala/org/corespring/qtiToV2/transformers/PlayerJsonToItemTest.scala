package org.corespring.qtiToV2.transformers

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.{ AdditionalCopyright, Copyright, Item }
import org.specs2.mutable.Specification
import play.api.libs.json.{JsString, Json}

class PlayerJsonToItemTest extends Specification {

  "PlayerJson => Item" should {

    "changes get picked up in the whole item" in {
      val json = Json.obj(
        "xhtml" -> JsString("joseph"),
        "components" -> Json.obj(
          "1" -> Json.obj(
            "componentType" -> "test-component"
          )
        ),
        "profile" -> Json.obj(
          "standards" -> Json.arr(
            Json.obj("dotNotation" -> "ST1"),
            Json.obj("dotNotation" -> "ST2")),
          "taskInfo" -> Json.obj(
            "title" -> "Drag and drop example",
            "gradeLevel" -> Json.arr("01", "03"),
            "subjects" -> Json.obj(
              "primary" -> Json.obj(
                "id" -> "4ffb535f6bb41e469c0bf2a8",
                "text" -> "Performing Arts"),
              "related" -> Json.arr(Json.obj(
                "id" -> "4ffb535f6bb41e469c0bf2a9",
                "text" -> "AP Music Theory,Visual Arts"))),
            "itemType" -> "Type"),
          "reviewsPassed" -> Json.arr("RP1", "RP2"),
          "reviewsPassedOther" -> "RPO",
          "priorGradeLevel" -> Json.arr("PGL1", "PGL2"),
          "priorUse" -> "PU",
          "priorUseOther" -> "PUO",
          "lexile" -> "30",
          "contributorDetails" -> Json.obj(
            "author" -> "AU",
            "contributor" -> "CON",
            "credentials" -> "CR",
            "credentialsOther" -> "CRO",
            "sourceUrl" -> "SU",
            "licenseType" -> "LT",
            "copyrightOwner" -> "OW",
            "copyrightYear" -> "1234",
            "copyrightExpirationDate" -> "2345",
            "additionalCopyrights" -> Json.arr(
              Json.obj(
                "author" -> "AU",
                "owner" -> "OW",
                "year" -> "YE",
                "licenseType" -> "LT",
                "mediaType" -> "MT",
                "sourceUrl" -> "SU"))),
          "otherAlignments" -> Json.obj(
            "bloomsTaxonomy" -> "BT",
            "keySkills" -> Json.arr("KS1", "KS2"),
            "depthOfKnowledge" -> "DOK")
        )
      )

      val item = Item()
      val update = PlayerJsonToItem.wholeItem(item, json)

      update.playerDefinition.get.xhtml must_== "joseph"
      update.playerDefinition.get.components must_== Json.obj(
        "1" -> Json.obj(
          "componentType" -> "test-component"
        )
      )

      update.standards === Seq("ST1", "ST2")

      update.taskInfo.map { info =>
        info.title === Some("Drag and drop example")
        info.itemType === Some("Type")
        info.gradeLevel === Seq("01", "03")
        info.subjects.get.primary === Some(new ObjectId("4ffb535f6bb41e469c0bf2a8"))
        info.subjects.get.related === Seq(new ObjectId("4ffb535f6bb41e469c0bf2a9"))
      }.getOrElse(failure("No updated info"))

      update.reviewsPassed === List("RP1", "RP2")
      update.reviewsPassedOther === Some("RPO")
      update.priorGradeLevels === List("PGL1", "PGL2")
      update.priorUse === Some("PU")
      update.priorUseOther === Some("PUO")
      update.lexile === Some("30")

      update.contributorDetails.map { details =>
        details.author === Some("AU")
        details.contributor === Some("CON")
        details.credentials === Some("CR")
        details.credentialsOther === Some("CRO")
        details.licenseType === Some("LT")
        details.sourceUrl === Some("SU")
        details.copyright === Some(Copyright(Some("OW"), Some("1234"), Some("2345"), None))
        details.additionalCopyrights.map { copyright =>
          copyright === AdditionalCopyright(Some("AU"), Some("OW"), Some("YE"), Some("LT"), Some("MT"), Some("SU"))
        }
      }.getOrElse(failure("No updated contributorDetails"))

      update.otherAlignments.map { alignments =>
        alignments.bloomsTaxonomy === Some("BT")
        alignments.keySkills === Seq("KS1", "KS2")
        alignments.depthOfKnowledge === Some("DOK")
      }.getOrElse(failure("No updated alignments"))

    }
    "work for profile " in {

      val json = Json.obj(
        "standards" -> Json.arr(
          Json.obj("dotNotation" -> "ST1"),
          Json.obj("dotNotation" -> "ST2")),
        "taskInfo" -> Json.obj(
          "title" -> "Drag and drop example",
          "gradeLevel" -> Json.arr("01", "03"),
          "subjects" -> Json.obj(
            "primary" -> Json.obj(
              "id" -> "4ffb535f6bb41e469c0bf2a8",
              "text" -> "Performing Arts"),
            "related" -> Json.arr(Json.obj(
              "id" -> "4ffb535f6bb41e469c0bf2a9",
              "text" -> "AP Music Theory,Visual Arts"))),
          "itemType" -> "Type"),
        "reviewsPassed" -> Json.arr("RP1", "RP2"),
        "reviewsPassedOther" -> "RPO",
        "priorGradeLevel" -> Json.arr("PGL1", "PGL2"),
        "priorUse" -> "PU",
        "priorUseOther" -> "PUO",
        "lexile" -> "30",
        "contributorDetails" -> Json.obj(
          "author" -> "AU",
          "contributor" -> "CON",
          "credentials" -> "CR",
          "credentialsOther" -> "CRO",
          "sourceUrl" -> "SU",
          "licenseType" -> "LT",
          "copyrightOwner" -> "OW",
          "copyrightYear" -> "1234",
          "copyrightExpirationDate" -> "2345",
          "additionalCopyrights" -> Json.arr(
            Json.obj(
              "author" -> "AU",
              "owner" -> "OW",
              "year" -> "YE",
              "licenseType" -> "LT",
              "mediaType" -> "MT",
              "sourceUrl" -> "SU"))),
        "otherAlignments" -> Json.obj(
          "bloomsTaxonomy" -> "BT",
          "keySkills" -> Json.arr("KS1", "KS2"),
          "depthOfKnowledge" -> "DOK"))

      val item = Item()
      val update = PlayerJsonToItem.profile(item, json)

      update.standards === Seq("ST1", "ST2")

      update.taskInfo.map { info =>
        info.title === Some("Drag and drop example")
        info.itemType === Some("Type")
        info.gradeLevel === Seq("01", "03")
        info.subjects.get.primary === Some(new ObjectId("4ffb535f6bb41e469c0bf2a8"))
        info.subjects.get.related === Seq(new ObjectId("4ffb535f6bb41e469c0bf2a9"))
      }.getOrElse(failure("No updated info"))

      update.reviewsPassed === List("RP1", "RP2")
      update.reviewsPassedOther === Some("RPO")
      update.priorGradeLevels === List("PGL1", "PGL2")
      update.priorUse === Some("PU")
      update.priorUseOther === Some("PUO")
      update.lexile === Some("30")

      update.contributorDetails.map { details =>
        details.author === Some("AU")
        details.contributor === Some("CON")
        details.credentials === Some("CR")
        details.credentialsOther === Some("CRO")
        details.licenseType === Some("LT")
        details.sourceUrl === Some("SU")
        details.copyright === Some(Copyright(Some("OW"), Some("1234"), Some("2345"), None))
        details.additionalCopyrights.map { copyright =>
          copyright === AdditionalCopyright(Some("AU"), Some("OW"), Some("YE"), Some("LT"), Some("MT"), Some("SU"))
        }
      }.getOrElse(failure("No updated contributorDetails"))

      update.otherAlignments.map { alignments =>
        alignments.bloomsTaxonomy === Some("BT")
        alignments.keySkills === Seq("KS1", "KS2")
        alignments.depthOfKnowledge === Some("DOK")
      }.getOrElse(failure("No updated alignments"))

    }

    "work for xhtml + components" in {

      val jsonString =
        """
          |{
          |  "xhtml" : "<div/>",
          |  "components" : {
          |    "1" : {
          |      "componentType" : "test-component"
          |    }
          |  },
          |  "summaryFeedback" : "some feedback"
          |}
        """.stripMargin

      val json = Json.parse(jsonString)
      val item = Item()
      val update = PlayerJsonToItem.playerDef(item, json)

      update.playerDefinition.get.xhtml === "<div/>"
      (update.playerDefinition.get.components \ "1" \ "componentType").as[String] === "test-component"
      update.playerDefinition.get.summaryFeedback === "some feedback"

    }
  }

}
