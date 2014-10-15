package org.corespring.importing

import org.specs2.mutable.Specification
import scala.io.Source
import org.corespring.test.PlaySingleton
import org.corespring.platform.core.models.item.Item

class ItemFileConverterTest extends Specification {

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
    "xhtml": "<itemBody><div><img src='dot array.png'/></div><p>Which of the following are equal to the number of dots in the picture? (Choose all that apply.)</p><corespring-multiple-choice id='Q_01'></corespring-multiple-choice></itemBody>"
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
    "lexile": "24",
    "otherAlignments": {
      "bloomsTaxonomy" : "Analyzing",
      "keySkills" : [
        "Define",
        "Discuss",
        "Distinguish",
        "Choose",
        "Analyze",
        "Examine"
      ],
      "relatedCurriculum" : "New Jersey Model Curriculum",
      "depthOfKnowledge" : "Strategic Thinking & Reasoning"
    },
    "pValue": "0",
    "priorUse" : "Summative",
    "priorGradeLevel": [
      "PK",
      "UG",
      "04"
    ],
    "reviewsPassed": [
      "Bias"
    ],
    "standards": [
      "RL.2.6"
    ],
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
      "gradeLevel" : [
        "03"
      ],
      "title" : "Read the passage <i>What's the Scoop on Soil?</i>. Which aspects of soil does the passage not explain? What can you tell about soil by squeezing it?",
      "description" : "Item about Soil"
    }
  }"""

  "convert" should {

    val sources = (Seq(
      "dot array.png", "metadata.json", "files/rubric.pdf"
    ).map(file => (file, Source.fromURL(getClass.getResource(s"/item/$file")))) ++
      Seq("item.json" -> Source.fromString(itemJson), "metadata.json" -> Source.fromString(metadataJson))).toMap

    val result = ItemFileConverter.convert(collectionId)(sources)

    "create Item from local files" in {
      result must beAnInstanceOf[Right[Error, Item]]
    }

    "item" should {

      val item: Item = result.asInstanceOf[Right[Error, Item]].b

      "have correct collectionId" in { item.collectionId must be equalTo(Some(collectionId)) }
      "have item as contentType" in { item.contentType must be equalTo(Item.contentType) }

      "contributorDetails" should {
        val itemContributorDetails = item.contributorDetails.getOrElse(throw new Exception("contributorDetails missing"))

        "have correct author" in { itemContributorDetails.author must be equalTo Some(contributorDetails.author) }
        "have correct contributor" in { itemContributorDetails.contributor must be equalTo Some(contributorDetails.contributor) }
      }

    }

  }

}
