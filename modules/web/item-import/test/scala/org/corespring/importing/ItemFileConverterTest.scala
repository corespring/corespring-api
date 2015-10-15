package org.corespring.importing

import org.apache.commons.io.IOUtils
import org.bson.types.ObjectId
import org.corespring.models.item.resource.{ BaseFile, StoredFile }
import org.corespring.models.item.{ FieldValue, Item, StringKeyValue }
import org.corespring.models.json.JsonFormatting
import org.corespring.models.{ Standard, Subject }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.io.Source
import scalaz.Success

class ItemFileConverterTest extends Specification with Mockito {

  val collectionId = "543edd2fa399191672bedea9"

  val itemId = new ObjectId()

  val storageKey = "storageKey"

  val files = Seq("dot array.png")

  private def toKeyValue(s: String) = StringKeyValue(s, s)

  val fieldValue = FieldValue(
    bloomsTaxonomy = Seq(otherAlignments.bloomsTaxonomy).map(toKeyValue),
    credentials = Seq(contributorDetails.credentials).map(toKeyValue),
    gradeLevels = Seq("03").map(toKeyValue))

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
    val depthOfKnowledge = "3"
    val keySkills = Seq("Define", "Discuss", "Distinguish", "Choose", "Analyze", "Examine")
    val relatedCurriculum = "New Jersey Model Curriculum"
  }

  val lexile = "24"
  val pValue = "0"
  val priorUse = "Summative"
  val priorGradeLevels = Seq("PG", "UG", "04")
  val reviewsPassed = Seq("Bias")
  val standards = Seq("RL.2.6")

  object supportingMaterial {
    val name = "Rubric"
    val files = Seq("files/rubric.pdf")
  }

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
    "files": ["${files.mkString("\",\"")}"],
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
        "name": "${supportingMaterial.name}",
        "files": [
          {
            "name": "${supportingMaterial.files.mkString("\",\"")}"
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

    val sources = (Seq(
      "dot array.png", "metadata.json", "files/rubric.pdf").map(file => (file, Source.fromURL(getClass.getResource(s"/item/$file"), "ISO-8859-1"))) ++
      Seq("item.json" -> Source.fromString(itemJson), "metadata.json" -> Source.fromString(metadataJson))).toMap

    val itemFileConverter = new ItemFileConverter {
      def bucket: String = "fake bucket"
      def itemService: ItemService = {
        import org.mockito.{ Matchers => MockitoMatchers }
        val service = mock[ItemService]
        service.insert(MockitoMatchers.anyObject().asInstanceOf[Item]).returns(Some(new VersionedId[ObjectId](id = new ObjectId(), version = Some(0))))
        service
      }
      def uploader = new Uploader {
        override def upload(filename: String, path: String, file: Source): Future[StoredFile] = future {
          StoredFile(name = filename, contentType = BaseFile.getContentType(filename), storageKey = storageKey)
        }
      }

      val defaultRootOrgId = ObjectId.get()

      override def jsonFormatting: JsonFormatting = new JsonFormatting {

        override def fieldValue: FieldValue = ItemFileConverterTest.this.fieldValue

        override def findStandardByDotNotation: (String) => Option[Standard] = s => None

        override def rootOrgId: ObjectId = defaultRootOrgId

        override def findSubjectById: (ObjectId) => Option[Subject] = id => None
      }

      override def readFile(path: String): Option[String] = {
        val root = this.getClass.getResource("/schema")
        println(s"root: $root")
        val url = this.getClass.getResource(s"/$path")
        require(url != null)
        println(s"--> url: $url")
        Some(IOUtils.toString(url))
      }
    }

    lazy val result = itemFileConverter.convert(collectionId)(sources)

    "create Item from local files" in {
      result must beAnInstanceOf[Success[Error, Item]]
    }

    "modules/web/item-import/test/resources/item" should {

      val item: Item = result.asInstanceOf[Success[Error, Item]].getOrElse(throw new Exception("no item"))

      "have correct collectionId" in { item.collectionId must be equalTo collectionId }
      "have item as contentType" in { item.contentType must be equalTo Item.contentType }

      "contributorDetails" should {
        val itemContributorDetails = item.contributorDetails.getOrElse(throw new Exception("contributorDetails missing"))

        "have correct author" in { itemContributorDetails.author must be equalTo Some(contributorDetails.author) }
        "have correct contributor" in { itemContributorDetails.contributor must be equalTo Some(contributorDetails.contributor) }

        "copyright" should {
          val itemCopyright = itemContributorDetails.copyright.getOrElse(throw new Exception("contributorDetails.copyright missing"))

          "have correct owner" in { itemCopyright.owner must be equalTo (Some(contributorDetails.copyright.owner)) }
          "have correct year" in { itemCopyright.year must be equalTo (Some(contributorDetails.copyright.year)) }
        }

        "have correct credentials" in { itemContributorDetails.credentials must be equalTo (Some(contributorDetails.credentials)) }
        "have correct licenseType" in { itemContributorDetails.licenseType must be equalTo (Some(contributorDetails.licenseType)) }
        "have correct sourceUrl" in { itemContributorDetails.sourceUrl must be equalTo (Some(contributorDetails.sourceUrl)) }

      }

      "data" should {
        val itemData = item.data.getOrElse(throw new Exception("data missing"))

        "have correct files" in { itemData.files.map(_.name) must be equalTo files }
      }

      "otherAlignments" should {
        val itemOtherAlignments = item.otherAlignments.getOrElse(throw new Exception("otherAlignments missing"))

        "have correct bloomsTaxonomy" in { itemOtherAlignments.bloomsTaxonomy must be equalTo Some(otherAlignments.bloomsTaxonomy) }
        "have correct keySkills" in { itemOtherAlignments.keySkills must be equalTo otherAlignments.keySkills }
        "have correct relatedCurriculum" in { itemOtherAlignments.relatedCurriculum must be equalTo Some(otherAlignments.relatedCurriculum) }
        "have correct depthOfKnowledge" in { itemOtherAlignments.depthOfKnowledge must be equalTo Some(otherAlignments.depthOfKnowledge) }
      }

      "have correct priorUse" in { item.priorUse must be equalTo (Some(priorUse)) }
      "have correct priorGradeLevels" in { item.priorGradeLevels must be equalTo (priorGradeLevels) }
      "have correct reviewsPassed" in { item.reviewsPassed must be equalTo (reviewsPassed) }
      "have correct standards" in { item.standards must be equalTo (standards) }

      "supportingMaterials" should {
        val itemSupportingMaterial = item.supportingMaterials.headOption.getOrElse(throw new Exception("supportingMaterial missing"))

        "have correct name" in {
          itemSupportingMaterial.name must be equalTo supportingMaterial.name
        }
        "have correct filename" in {
          itemSupportingMaterial.files.map(_.name) must be equalTo supportingMaterial.files
        }
        "have correct storageKey" in {
          itemSupportingMaterial.files.map(_.asInstanceOf[StoredFile].storageKey) must be equalTo Seq(storageKey)
        }
      }

      "taskInfo" should {
        val itemTaskInfo = item.taskInfo.getOrElse(throw new Exception("taskInfo missing"))

        "have correct gradeLevel" in { itemTaskInfo.gradeLevel must be equalTo taskInfo.gradeLevel }
        "have correct description" in { itemTaskInfo.description must be equalTo Some(taskInfo.description) }
        "have correct title" in { itemTaskInfo.title must be equalTo Some(taskInfo.title) }
      }

    }

  }

}
