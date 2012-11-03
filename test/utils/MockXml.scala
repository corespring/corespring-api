package utils

import xml.{Elem, NodeSeq}

object MockXml {

  val AllItems = scala.xml.XML.loadFile("test/mockXml/all-items.xml")

  val incorrectResponseFeedback = "incorrect response feedback"
  val correctResponseFeedback = "correct response feedback"

  def createXml(identifier:String, cardinality: String, values: NodeSeq, interaction: NodeSeq = <none/>): Elem = {
    <assessmentItem>
      <correctResponseFeedback>{correctResponseFeedback}</correctResponseFeedback>
      <incorrectResponseFeedback>{incorrectResponseFeedback}</incorrectResponseFeedback>
      <responseDeclaration identifier={identifier} cardinality={cardinality} baseType="string">
        <correctResponse>
          {values}
        </correctResponse>
      </responseDeclaration>
      <itemBody>
        {interaction}
      </itemBody>
    </assessmentItem>
  }

}
