package org.corespring.importing

import org.specs2.mutable.Specification
import scala.io.Source
import org.corespring.test.PlaySingleton
import org.corespring.platform.core.models.item.Item
import org.corespring.orgWithAccessToken
import org.corespring.amazon.s3.S3Service
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.{PlayerAccessSettings, AuthMode, OrgAndOpts}
import org.corespring.amazon.s3.models.DeleteResponse
import play.api.mvc._
import org.specs2.mock.Mockito
import scalaz.{Validation, Success}
import org.mockito.Matchers._
import scala.Some
import play.api.mvc.SimpleResult
import org.corespring.amazon.s3.models.DeleteResponse
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.platform.data.mongo.models.VersionedId
import org.bson.types.ObjectId
import org.corespring.v2.errors.V2Error

class ItemFileConverterTest extends Specification with Mockito {

  PlaySingleton.start()

  val collectionId = "543edd2fa399191672bedea9"

  object contributorDetails {
    val author = "State of New Jersey Department of Education"
    val contributor = "State of New Jersey Department of Education"

    object copyright {
      val owner = "State of New Jersey Department of Education"
      val year = "2012"
    }

    val credentials = "State Department of Education"
    val licenseType = "CC BY"
    val sourceUrl = "http://www.state.nj.us/education/modelcurriculum/ela/10u1.shtml"
  }

  object otherAlignments {
    val bloomsTaxonomy = "Analyzing"
    val depthOfKnowledge = "Strategic Thinking & Reasoning"
    val keySkills = Seq("Define", "Discuss", "Distinguish", "Choose", "Analyze", "Examine")
    val relatedCurriculum = "New Jersey Model Curriculum"
  }

  val lexile = "24"
  val pValue = "0"
  val priorUse = "Summative"
  val priorGradeLevels = Seq("PG", "UG", "04")
  val reviewsPassed = Seq("Bias")
  val standards = Seq("RL.2.6")

  object taskInfo {
    val gradeLevel = Seq("03")
    val title = "Read the passage <i>What's the Scoop on Soil?</i>. Which aspects of soil does the passage not explain? What can you tell about soil by squeezing it?"
    val description = "Item about Soil"
  }

  val xhtml = "<itemBody><div><img src='dot array.png'/></div><p>Which of the following are equal to the number of dots in the picture? (Choose all that apply.)</p><corespring-multiple-choice id='Q_01'></corespring-multiple-choice></itemBody>"

  val itemJson = s"""{
    "components": {
      "Q_01": {
        "componentType": "corespring-multiple-choice",
        "model": {
          "config": {
            "shuffle": "false",
            "orientation": "vertical",
            "choiceType": "checkbox",
            "choiceLabels": "letters",
            "singleChoice": false
          },
          "prompt": "Which of the following are equal to the number of dots in the picture? (Choose all that apply.)",
          "choices": [
            {
              "label": "3 + 3 + 3",
              "value": "ChoiceA"
            },
            {
              "label": "3 + 4",
              "value": "ChoiceB"
            },
            {
              "label": "4 + 4 + 4",
              "value": "ChoiceC"
            },
            {
              "label": "4 + 4 + 4 + 4",
              "value": "ChoiceD"
            },
            {
              "label": "3 + 3 + 3 + 3",
              "value": "ChoiceE"
            }
          ],
          "feedback": [
            {
              "value": "ChoiceA",
              "feedback": "Your Answer",
              "notChosenFeedback": "Your Answer"
            },
            {
              "value": "ChoiceB",
              "feedback": "Your Answer",
              "notChosenFeedback": "Your Answer"
            },
            {
              "value": "ChoiceC",
              "feedback": "Correct Answer",
              "notChosenFeedback": "Correct Answer"
            },
            {
              "value": "ChoiceD",
              "feedback": "Your Answer",
              "notChosenFeedback": "Your Answer"
            },
            {
              "value": "ChoiceE",
              "feedback": "Correct Answer",
              "notChosenFeedback": "Correct Answer"
            }
          ],
          "correctResponse": {
            "value": [
              "ChoiceC",
              "ChoiceE"
            ]
          }
        }
      }
    },
    "files": [
      "dot array.png"
    ],
    "xhtml": "$xhtml"
  }"""

  val metadataJson = s"""{
    "contributorDetails": {
      "additionalCopyrights": [
        {
          "owner": "Ben"
        }
      ],
      "author" : "${contributorDetails.author}",
      "contributor" : "${contributorDetails.contributor}",
      "copyright": {
        "owner": "${contributorDetails.copyright.owner}",
        "year": "${contributorDetails.copyright.year}"
      },
      "credentials" : "${contributorDetails.credentials}",
      "licenseType": "${contributorDetails.licenseType}",
      "sourceUrl" : "${contributorDetails.sourceUrl}"
    },
    "lexile": "$lexile",
    "otherAlignments": {
      "bloomsTaxonomy" : "${otherAlignments.bloomsTaxonomy}",
      "keySkills" : [ "${otherAlignments.keySkills.mkString("\",\"")}" ],
      "relatedCurriculum" : "${otherAlignments.relatedCurriculum}",
      "depthOfKnowledge" : "${otherAlignments.depthOfKnowledge}"
    },
    "pValue": "$pValue",
    "priorUse" : "$priorUse",
    "priorGradeLevels": ["${priorGradeLevels.mkString("\",\"")}"],
    "reviewsPassed": ["${reviewsPassed.mkString("\",\"")}"],
    "standards": ["${standards.mkString("\",\"")}"],
    "supportingMaterials": [
      {
        "name": "Rubric",
        "files": [
          {
            "name": "files/rubric.pdf"
          }
        ]
      }
    ],
    "taskInfo": {
      "gradeLevel" : ["${taskInfo.gradeLevel.mkString("\",\"")}"],
      "title" : "${taskInfo.title}",
      "description" : "${taskInfo.description}"
    }
  }"""

  "convert" should {

    trait withResult extends orgWithAccessToken {

      val sources = (Seq(
        "dot array.png", "metadata.json", "files/rubric.pdf"
      ).map(file => (file, Source.fromURL(getClass.getResource(s"/item/$file"), "ISO-8859-1"))) ++
        Seq("item.json" -> Source.fromString(itemJson), "metadata.json" -> Source.fromString(metadataJson))).toMap

      implicit val identity = OrgAndOpts(orgId = orgId, opts = PlayerAccessSettings.ANYTHING, authMode = AuthMode.AccessToken)

      val itemFileConverter = new ItemFileConverter {
        def s3: S3Service = new S3Service {
          def download(bucket: String, fullKey: String, headers: Option[Headers]): SimpleResult = ???
          def upload(bucket: String, keyName: String, predicate: (RequestHeader) => Option[SimpleResult]): BodyParser[Int] = ???
          def delete(bucket: String, keyName: String): DeleteResponse = ???
        }
        def bucket: String = "fake bucket"
        def auth: ItemAuth[OrgAndOpts] = new ItemAuth[OrgAndOpts] {
          def canCreateInCollection(collectionId: String)(implicit identity: OrgAndOpts): Validation[V2Error, Boolean] = Success(true)
          def loadForRead(itemId: String)(implicit identity: OrgAndOpts): Validation[V2Error, Item] = ???
          def loadForWrite(itemId: String)(implicit identity: OrgAndOpts): Validation[V2Error, Item] = ???
          def save(item: Item, createNewVersion: Boolean)(implicit identity: OrgAndOpts): Unit = {}
          def insert(item: Item)(implicit identity: OrgAndOpts): Option[VersionedId[ObjectId]] = ???
        }

      }
      val result = itemFileConverter.convert(collectionId, identity)(sources)
    }


    "create Item from local files" in new withResult {
      result must beAnInstanceOf[Right[Error, Item]]
    }

    "item" should {

      trait withItem extends withResult {
        val item: Item = result.asInstanceOf[Right[Error, Item]].b
      }
      "have correct collectionId" in new withItem { item.collectionId must be equalTo(Some(collectionId)) }
      "have item as contentType" in new withItem { item.contentType must be equalTo(Item.contentType) }

      "contributorDetails" should {
        trait withContributorDetails extends withItem {
          val itemContributorDetails = item.contributorDetails.getOrElse(throw new Exception("contributorDetails missing"))
        }
        "have correct author" in new withContributorDetails { itemContributorDetails.author must be equalTo Some(contributorDetails.author) }
        "have correct contributor" in new withContributorDetails { itemContributorDetails.contributor must be equalTo Some(contributorDetails.contributor) }

        "copyright" should {
          trait withCopyright extends withContributorDetails {
            val itemCopyright = itemContributorDetails.copyright.getOrElse(throw new Exception("contributorDetails.copyright missing"))
          }
          "have correct owner" in new withCopyright { itemCopyright.owner must be equalTo(Some(contributorDetails.copyright.owner)) }
          "have correct year" in new withCopyright { itemCopyright.year must be equalTo(Some(contributorDetails.copyright.year)) }
        }

        "have correct credentials" in new withContributorDetails { itemContributorDetails.credentials must be equalTo(Some(contributorDetails.credentials)) }
        "have correct licenseType" in new withContributorDetails { itemContributorDetails.licenseType must be equalTo(Some(contributorDetails.licenseType)) }
        "have correct sourceUrl" in new withContributorDetails { itemContributorDetails.sourceUrl must be equalTo(Some(contributorDetails.sourceUrl)) }

      }

      "otherAlignments" should {
        trait withOtherAlignments extends withItem {
          val itemOtherAlignments = item.otherAlignments.getOrElse(throw new Exception("otherAlignments missing"))
        }

        "have correct bloomsTaxonomy" in new withOtherAlignments { itemOtherAlignments.bloomsTaxonomy must be equalTo Some(otherAlignments.bloomsTaxonomy) }
        "have correct keySkills" in new withOtherAlignments { itemOtherAlignments.keySkills must be equalTo otherAlignments.keySkills }
        "have correct relatedCurriculum" in new withOtherAlignments { itemOtherAlignments.relatedCurriculum must be equalTo Some(otherAlignments.relatedCurriculum) }
        "have correct depthOfKnowledge" in new withOtherAlignments { itemOtherAlignments.depthOfKnowledge must be equalTo Some(otherAlignments.depthOfKnowledge) }
      }

      "have correct priorUse" in new withItem { item.priorUse must be equalTo(Some(priorUse)) }
      "have correct priorGradeLevels" in new withItem { item.priorGradeLevels must be equalTo(priorGradeLevels) }
      "have correct reviewsPassed" in new withItem { item.reviewsPassed must be equalTo(reviewsPassed) }
      "have correct standards" in new withItem { item.standards must be equalTo(standards) }

      "taskInfo" should {
        trait withTaskInfo extends withItem {
          val itemTaskInfo = item.taskInfo.getOrElse(throw new Exception("taskInfo missing"))
        }

        "have correct gradeLevel" in new withTaskInfo { itemTaskInfo.gradeLevel must be equalTo taskInfo.gradeLevel }
        "have correct description" in new withTaskInfo { itemTaskInfo.description must be equalTo Some(taskInfo.description) }
        "have correct title" in new withTaskInfo { itemTaskInfo.title must be equalTo Some(taskInfo.title) }
      }

    }

  }

}
