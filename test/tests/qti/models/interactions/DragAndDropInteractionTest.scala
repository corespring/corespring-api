package tests.qti.models.interactions

import org.specs2.mutable._
import qti.models.interactions.DragAndDropInteraction
import qti.models.{ResponseDeclaration, QtiItem}
import utils.MockXml
import utils.MockXml.{removeNodeFromXmlWhere, addChildNodeInXmlWhere}
import org.corespring.platform.core.models.itemSession.ArrayItemResponse

class DragAndDropInteractionTest extends Specification {


  def interactionXml = MockXml.load("drag-and-drop-mock.xml")

  "Drag and Drop Interaction" should {

    val interaction: DragAndDropInteraction = QtiItem(interactionXml).itemBody.interactions.find(_.responseIdentifier == "alphabet1").get.asInstanceOf[DragAndDropInteraction]

    "parses" in {
      interaction mustNotEqual null
    }

    "collects answers" in {
      interaction.choices.length mustEqual 6
    }

    "collects ordered nodes" in {
      interaction.targets.filter(_.cardinality == "ordered").map(_.identifier).toSet mustEqual Set("target1", "target4")
    }

    "validate drag and drop interaction" in {
      val item = QtiItem(interactionXml)
      item.isQtiValid._1 mustEqual true
    }

    "populates responseCorrect" in {
      val item = QtiItem(interactionXml)
      val rd = item.responseDeclarations.find(_.identifier == "alphabet1")
      val response = ArrayItemResponse("alphabet1", Seq("target1:apple|pear","target2:cow","target3:car|bus","target4:apple|pear"))
      val outcome = interaction.getOutcome(rd, response).get
      outcome.outcomeProperties.get("responseCorrect").get must beTrue
      outcome.outcomeProperties.get("responseIncorrect").get must beFalse
    }

    "populates responseIncorrect" in {
      val item = QtiItem(interactionXml)
      val rd = item.responseDeclarations.find(_.identifier == "alphabet1")
      val response = ArrayItemResponse("alphabet1", Seq("target1:apple|pear","target2:pear","target3:car|bus","target4:apple|pear"))
      val outcome = interaction.getOutcome(rd, response).get
      outcome.outcomeProperties.get("responseCorrect").get must beFalse
      outcome.outcomeProperties.get("responseIncorrect").get must beTrue
    }

    "drag and drop interaction answer validation" in {
      val xml2 = addChildNodeInXmlWhere(interactionXml, <value>frog</value>) {
        e =>
          e.attribute("identifier") match {
            case Some(id) => id.text == "target3"
            case _ => false
          }
      }

      QtiItem(xml2) must throwAn[IllegalArgumentException]
    }

    "drag and drop interaction target validation #1" in {
      val xml = removeNodeFromXmlWhere(interactionXml) {
        n => n.label != DragAndDropInteraction.targetNodeLabel && (n.attribute("identifier") match {
          case Some(id) => id.text == "target2"
          case _ => false
        })
      }
      QtiItem(xml) must throwAn[IllegalArgumentException]
    }

    "drag and drop interaction target validation #2" in {
      QtiItem(removeNodeFromXmlWhere(interactionXml) {
        n =>
          (n \ "@identifier").text == "target2" && n.label == DragAndDropInteraction.targetNodeLabel
      }) must throwAn[IllegalArgumentException]
    }

  }

}
