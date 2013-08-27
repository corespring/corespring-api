package org.corespring.qti.models.interactions

import org.corespring.qti.models.responses.ArrayResponse
import org.corespring.qti.models.{ ResponseDeclaration, QtiItem }
import org.specs2.mutable.Specification

class LineInteractionTest extends Specification {

  val qti = QtiItem(
    <assessmentItem>
      <responseDeclaration identifier="graphTest" baseType="line" cardinality="single">
        <correctResponse>
          <value>y=2x+7</value>
        </correctResponse>
      </responseDeclaration>
      <itemBody>
        <lineInteraction jsxgraphcore="" responseIdentifier="graphTest" graph-width="300px" graph-height="300px" domain="10" range="10" scale="1" domain-label="x" range-label="y" tick-label-frequency="5"></lineInteraction>
      </itemBody>
    </assessmentItem>)

  val optli1 = qti.itemBody.interactions.find(i => i.responseIdentifier == "graphTest" && i.isInstanceOf[LineInteraction]).map(_.asInstanceOf[LineInteraction])

  val optrd1 = qti.responseDeclarations.find(_.identifier == "graphTest")

  val qti2 = QtiItem(
    <assessmentItem>
      <itemBody>
        <lineInteraction jsxgraphcore="" responseIdentifier="graphTest" locked=""></lineInteraction>
      </itemBody>
    </assessmentItem>)

  val optli2 = qti2.itemBody.interactions.find(i => i.responseIdentifier == "graphTest" && i.isInstanceOf[LineInteraction]).map(_.asInstanceOf[LineInteraction])

  val optrd2 = qti2.responseDeclarations.find(_.identifier == "graphTest")

  val qti3 = QtiItem(
    <assessmentItem>
      <responseDeclaration identifier="graphTest" baseType="line" cardinality="single">
        <correctResponse>
          <value>y=1.571x+3</value>
        </correctResponse>
      </responseDeclaration>
      <itemBody>
        <lineInteraction jsxgraphcore="" responseIdentifier="graphTest" sigfigs="3"></lineInteraction>
      </itemBody>
    </assessmentItem>)

  val optli3 = qti3.itemBody.interactions.find(i => i.responseIdentifier == "graphTest" && i.isInstanceOf[LineInteraction]).map(_.asInstanceOf[LineInteraction])

  val optrd3 = qti3.responseDeclarations.find(_.identifier == "graphTest")

  "qti item 1" should {
    "contain line interaction" in {
      optli1 must beSome[LineInteraction]
    }
    "contain response declaration" in {
      optrd1 must beSome[ResponseDeclaration]
    }
  }

  "line interaction 1" should {
    "not be locked" in {
      optli1.map(_.locked) must beSome(false)
    }
    "not contain significant figures" in {
      optli1.map(_.sigfigs) must beSome(-1)
    }
    "be answered correctly" in {
      optli1.flatMap(_.getOutcome(optrd1, ArrayResponse("graphTest", Seq("-3,1", "-2,3")))).map(_.isCorrect) must beSome(true)
    }
    "be answered correctly with different coordinates" in {
      optli1.flatMap(_.getOutcome(optrd1, ArrayResponse("graphTest", Seq("1,9", "-4,-1")))).map(_.isCorrect) must beSome(true)
    }
  }

  "line interaction 2" should {
    "not be locked" in {
      optli2.map(_.locked) must beSome(true)
    }
  }

  "line interaction 3" should {
    "contain significant figures" in {
      optli3.map(_.sigfigs) must beSome(3)
    }
    "be answered correctly when slope matches significant figures" in {
      optli3.flatMap(_.getOutcome(optrd3, ArrayResponse("graphTest", Seq("1,4.571428571", "3,7.714285713")))).map(_.isCorrect) must beSome(true)
    }
  }

}
