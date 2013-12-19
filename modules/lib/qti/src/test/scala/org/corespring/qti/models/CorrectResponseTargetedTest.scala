package org.corespring.qti.models

import org.specs2.mutable.Specification
import scala.xml.{Elem, NodeSeq}
import org.corespring.qti.helpers.MockXml

// TODO This needs a bit of cleanup
class CorrectResponseTargetedTest extends Specification {

  def xml(identifier: String, cardinality: String, values: NodeSeq, interaction: NodeSeq = <none/>): Elem = MockXml.createXml(identifier, cardinality, values, interaction)

  "targeted correctness" in {
    val item = QtiItem(
      xml("id", "targeted",
        <value identifier="target1">
          <value>apple</value>
          <value>pear</value>
          <value>cherry</value>
          <value>orange</value>
        </value>
          <value identifier="target2">
            <value>cow</value>
            <value>bear</value>
            <value>fox</value>
          </value>
          <value identifier="target3">
            <value>apple</value>
          </value>
          <value identifier="target4">
            <value>fox</value>
          </value>,
        <dragAndDropInteraction responseIdentifier="id">
          <draggableChoice identifier="apple">Apple</draggableChoice>
          <draggableChoice identifier="pear">Pear</draggableChoice>
          <draggableChoice identifier="cherry">Cherry</draggableChoice>
          <draggableChoice identifier="orange">Orange</draggableChoice>
          <draggableChoice identifier="cow">Cow</draggableChoice>
          <draggableChoice identifier="bear">Bear</draggableChoice>
          <draggableChoice identifier="fox">Fox</draggableChoice>
          <landingPlace cardinality="ordered" identifier="target1"></landingPlace>
          <landingPlace cardinality="multiple" identifier="target2"></landingPlace>
          <landingPlace cardinality="single" identifier="target3"></landingPlace>
          <landingPlace cardinality="ordered" identifier="target4"></landingPlace>
        </dragAndDropInteraction>))
    val response = item.responseDeclarations(0).correctResponse.get
    response.isValueCorrect("target1:apple|pear|cherry|orange", None) === true
    response.isValueCorrect("target1:apple|pear|cherry", None) === false
    response.isValueCorrect("target1:pear|apple|cherry|orange", None) === false
    response.isValueCorrect("target2:cow|bear|fox", None) === true
    response.isValueCorrect("target2:fox|bear|cow", None) === true
    response.isCorrect("target1:apple|pear|cherry|orange,target2:fox|bear|cow,target3:apple,target4:fox") mustEqual true
    response.isCorrect("target1:pear|apple|cherry|orange,target2:cow|bear|fox,target3:apple,target4:fox") mustEqual false
  }

}
