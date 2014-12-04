package org.corespring.qtiToV2.kds

import org.specs2.mutable.Specification

import scala.xml.Node

class ProcessingTransformerTest extends Specification with ProcessingTransformer {

  implicit val emptyNode = <noOp/>

  "_match" should {

    val responseId = "RESPONSE1"
    val correctResponses = Seq("2")

    def qti(responseId: String = responseId,
            correctResponses: Seq[String] = correctResponses) =
      <assessmentItem>
        <responseDeclaration identifier={responseId} cardinality="single" baseType="identifier">
          <correctResponse>
            {correctResponses.map(r => <value>{r}</value>)}
          </correctResponse>
        </responseDeclaration>
        <responseProcessing>
          <match>
            <variable identifier={responseId}/>
            <correct identifier={responseId}/>
          </match>
        </responseProcessing>
      </assessmentItem>

    def matcher(qti: Node) =
      (qti \\ "responseProcessing").headOption.getOrElse(throw new Exception("Does not have response processing"))
        .child.find(_.label == "match").getOrElse("Does not have match node").asInstanceOf[Node]

    "translate equivalence of variable to single correct value" in {
      val node = qti()
      val matchNode = matcher(node)
      _match(matchNode)(node) must be equalTo(s"""$responseId == "${correctResponses.head}"""")
    }

    "translate equivalence of variable to multiple correct values" in {
      val correctResponses = Seq("a", "b")
      val node = qti(correctResponses = correctResponses)
      val matchNode = matcher(node)
      _match(matchNode)(node) must be equalTo(s"""["${correctResponses.mkString("\",\"")}"].indexOf($responseId) >= 0""")
    }

  }


  val oneExpression = Seq(<match><variable identifier="test"/><variable identifier="test"/></match>)
  val twoExpressions = oneExpression :+ <match><variable identifier="one"/><variable identifier="two"/></match>
  val threeExpressions = twoExpressions :+ <match><variable identifier="great"/><variable identifier="stuff"/></match>
  def node(expressions: Seq[Node]) = <and>{expressions}</and>

  "and" should {

    "error on empty node" in {
      and(<and/>) must throwAn[Exception]
    }

    "error on one expression" in {
      and(node(oneExpression)) must throwAn[Exception]
    }

    "combine two expressions" in {
      and(node(twoExpressions)) must be equalTo(s"""${twoExpressions.map(expression(_)).mkString(" && ")}""")
    }

    "combine three expressions" in {
      and(node(threeExpressions)) must be equalTo(s"""${threeExpressions.map(expression(_)).mkString(" && ")}""")
    }

  }

  "and" should {

    def node(expressions: Seq[Node]) = <or>{expressions}</or>

    "error on empty node" in {
      or(<or/>) must throwAn[Exception]
    }

    "error on one expression" in {
      or(node(oneExpression)) must throwAn[Exception]
    }

    "combine two expressions" in {
      or(node(twoExpressions)) must be equalTo(s"""${twoExpressions.map(expression(_)).mkString(" || ")}""")
    }

    "combine three expressions" in {
      or(node(threeExpressions)) must be equalTo(s"""${threeExpressions.map(expression(_)).mkString(" || ")}""")
    }

  }

  "setOutcomeValue" should {

    val identifier = "RESPONSE1"
    val value = "OMG"

    val node =
      <setOutcomeValue identifier={identifier}>
        <baseValue>{value}</baseValue>
      </setOutcomeValue>

    "translate into assignment expression" in {
      setOutcomeValue(node) must be equalTo(s"""$identifier = "$value";""")
    }

  }

  "responseIf" should {

    val identifier = "SCORE"
    val value = "great"
    val responseId = "RESPONSE1"
    val correctResponses = Seq("2")

    def qti(responseId: String = responseId,
            correctResponses: Seq[String] = correctResponses,
            responseIfNode: Node) =
      <assessmentItem>
        <responseDeclaration identifier={responseId} cardinality="single" baseType="identifier">
          <correctResponse>
            {correctResponses.map(r => <value>{r}</value>)}
          </correctResponse>
        </responseDeclaration>
        <responseProcessing>
          {responseIfNode}
        </responseProcessing>
      </assessmentItem>

    def responseIfNode() =
      <responseIf>
        <match>
          <variable identifier={responseId}/>
          <correct identifier={responseId}/>
        </match>
        <setOutcomeValue identifier={identifier}>
          <baseValue>{value}</baseValue>
        </setOutcomeValue>
      </responseIf>

    "translate into if statement" in {
      val responseIfNodeVal = responseIfNode()
      val qtiNode = qti(responseIfNode = responseIfNodeVal)
      responseIf(responseIfNodeVal)(qtiNode) must be equalTo s"""if ($responseId == "${correctResponses.head}") { $identifier = "$value"; }"""
    }

  }

}
