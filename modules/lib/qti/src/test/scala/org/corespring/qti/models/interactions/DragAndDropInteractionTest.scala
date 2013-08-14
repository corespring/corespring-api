package org.corespring.qti.models.interactions

import org.corespring.qti.models.responses.ArrayResponse
import org.corespring.qti.models.QtiItem
import org.specs2.mutable._
import utils.MockXml
import utils.MockXml.{removeNodeFromXmlWhere, addChildNodeInXmlWhere}

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
      val response = ArrayResponse("alphabet1", Seq("target1:apple|pear","target2:cow","target3:car|bus","target4:apple|pear"))
      val outcome = interaction.getOutcome(rd, response).get
      outcome.outcomeProperties.get("responseCorrect").get must beTrue
      outcome.outcomeProperties.get("responseIncorrect").get must beFalse
    }

    "populates responseIncorrect" in {
      val item = QtiItem(interactionXml)
      val rd = item.responseDeclarations.find(_.identifier == "alphabet1")
      val response = ArrayResponse("alphabet1", Seq("target1:apple|pear","target2:pear","target3:car|bus","target4:apple|pear"))
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

  val elems = List("apple","pear","moon","shine","bell","head")

  def isFixed(e:String) = e.contains("a")
  val iterations = 100
  import DragAndDropInteraction._
  "isTrue" should {

    "test boolean" in {
      isTrue(true) must beTrue
      isTrue(false) must beFalse
    }

    "test string" in {
      isTrue("true") must beTrue
      isTrue("TrUe") must beTrue
      isTrue("") must beFalse
      isTrue("false") must beFalse
    }

    "test node" in {
      val trueNode = <node id="true"></node>
      val falseNode = <node id="false"></node>
      isTrue(trueNode.attribute("id").get) must beTrue
      isTrue(falseNode.attribute("id").get) must beFalse
    }

    "test option" in {
      isTrue(Some(true)) must beTrue
      isTrue(Some(false)) must beFalse
      isTrue(None) must beFalse
    }

    "test option node" in {
      val trueNode = <node id="true"></node>
      val falseNode = <node id="false"></node>
      isTrue(trueNode.attribute("id")) must beTrue
      isTrue(falseNode.attribute("id")) must beFalse
    }

  }

  "shuffle" should {

    val results = collection.mutable.Map[String, Int]()
    elems.foreach(results(_) = 0)
    (1 to iterations) foreach { i=>
      val shuffled = shuffleElements(elems, isFixed)
      shuffled.zipWithIndex foreach { case(el, i) => results(el) = results(el) + i }
    }

    "not touch fixed elements" in {
      results("apple") mustEqual 0
      results("pear") mustEqual iterations
      results("head") mustEqual 5 * iterations
    }

    "distribute evenly" in {
      println(results)
      results("moon") must beCloseTo(300,30)
      results("shine") must beCloseTo(300,30)
      results("bell") must beCloseTo(300,30)
    }
  }

}
