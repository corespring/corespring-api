package org.corespring.qtiToV2.kds

import org.specs2.mutable.Specification

import scala.xml.Node

class ProcessingTransformerTest extends Specification with ProcessingTransformer with V2JavascriptWrapper {

  implicit val emptyNode = <noOp/>

  "_match" should {

    val responseId = "RESPONSE1"
    val correctResponses = Seq("2")

    def qti(responseId: String = responseId,
            correctResponses: Seq[String] = correctResponses) =
      <assessmentItem>
        <responseDeclaration identifier={responseId} cardinality="single" baseType="string">
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
      _match(matchNode)(node) must be equalTo(s"""$responseId === "${correctResponses.head}"""")
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

  "sum" should {
    val values = Seq("1", "2")
    val node = <sum>{values.map(v => <baseValue>{v}</baseValue>)}</sum>

    "return X + Y" in {
      sum(node) must be equalTo values.mkString(" + ")
    }
  }

  "gt" should {
    val values = Seq("2", "1")
    val node = <gt>{values.map(v => <baseValue>{v}</baseValue>)}</gt>

    "return X > Y" in {
      gt(node) must be equalTo values.mkString(" > ")
    }
  }

  "setOutcomeValue" should {

    val identifier = "RESPONSE1"
    val value = "OMG"

    val node =
      <setOutcomeValue identifier={identifier}>
        <baseValue baseType="string">{value}</baseValue>
      </setOutcomeValue>

    "translate into assignment expression" in {
      setOutcomeValue(node) must be equalTo(s"""$identifier = "$value";""")
    }

  }

  "responseIf" should {

    val identifier = "SCORE"
    val value = "great"
    val responseId = "RESPONSE1"
    val correctResponses = Seq("RESPONSE2")

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
          <responseCondition>
            {responseIfNode}
          </responseCondition>
        </responseProcessing>
      </assessmentItem>

    def responseIfNode() =
      <responseIf>
        <match>
          <variable identifier={responseId}/>
          <correct identifier={responseId}/>
        </match>
        <setOutcomeValue identifier={identifier}>
          <baseValue baseType="string">{value}</baseValue>
        </setOutcomeValue>
      </responseIf>

    "translate into if statement" in {
      val responseIfNodeVal = responseIfNode()
      val qtiNode = qti(responseIfNode = responseIfNodeVal)
      responseIf(responseIfNodeVal)(qtiNode) must be equalTo s"""if ($responseId === ${correctResponses.head}) { $identifier = "$value"; }"""
    }

  }

  "responseCondition" should {
    val responseConditionVal =
      <responseCondition>
        <responseIf>
          <and>
            <match>
              <variable identifier="RESPONSE1"/>
              <correct identifier="RESPONSE1"/>
            </match>
            <match>
              <variable identifier="RESPONSE2"/>
              <correct identifier="RESPONSE2"/>
            </match>
          </and>
          <setOutcomeValue identifier="SCORE">
            <baseValue baseType="float">2</baseValue>
          </setOutcomeValue>
        </responseIf>
        <responseElseIf>
          <match>
            <variable identifier="RESPONSE1"/>
            <correct identifier="RESPONSE1"/>
          </match>
          <setOutcomeValue identifier="SCORE">
            <baseValue baseType="float">1</baseValue>
          </setOutcomeValue>
        </responseElseIf>
      </responseCondition>

    def qti(responseConditionVal: Node = responseConditionVal) =
      <assessmentItem>
        <responseDeclaration identifier="RESPONSE1" cardinality="single" baseType="string">
          <correctResponse>
            <value>ONE</value>
          </correctResponse>
        </responseDeclaration>
        <responseDeclaration identifier="RESPONSE2" cardinality="single" baseType="string">
          <correctResponse>
            <value>TWO</value>
          </correctResponse>
        </responseDeclaration>
        <responseProcessing>
          {responseConditionVal}
        </responseProcessing>
      </assessmentItem>

      "translate into conditional statement" in {
        responseCondition(responseConditionVal)(qti()) must be equalTo
          """if ((RESPONSE1 === "ONE") && (RESPONSE2 === "TWO")) { SCORE = 2; } else if (RESPONSE1 === "ONE") { SCORE = 1; }"""
      }
  }

  "toJs" should {

    val qti =
      <assessmentItem>
        <responseDeclaration identifier="RESPONSE11" cardinality="single" baseType="string">
          <correctResponse>
            <value>3.89</value>
          </correctResponse>
        </responseDeclaration>
        <responseDeclaration identifier="RESPONSE21" cardinality="single" baseType="string">
          <correctResponse>
            <value>104</value>
            <value>104.00</value>
          </correctResponse>
        </responseDeclaration>
        <outcomeDeclaration identifier="NUMCORRECT" cardinality="single" baseType="float">
          <defaultValue>
            <value>0</value>
          </defaultValue>
        </outcomeDeclaration>
        <outcomeDeclaration identifier="SCORE" cardinality="single" baseType="float">
          <defaultValue>
            <value>0</value>
          </defaultValue>
        </outcomeDeclaration>
        <itemBody>
          <strong>
            <p>Part A:</p>
            <p>The total cost of an order of personalized invitations from a company includes the cost of each invitation, plus a one&#8211;time design fee. &nbsp;The cost of each invitation is the same regardless of how many invitations are ordered.</p>
            <p>The invitation company provides the following examples to customers in order to estimate the total cost of an order:</p>
          </strong>
          <ul>
            <li>
              <strong>50 invitations $298.50</strong>
            </li>
            <li>
              <strong>500 invitations $2049</strong>
            </li>
          </ul>
          <strong>Based on the examples, what is the cost of each invitation, not including the one&#8211;time design fee?</strong>
          <strong>$</strong>
          <textEntryInteraction responseIdentifier="RESPONSE11" expectedLength="10"/>
          <strong>
            <p>Part B:</p>
            <p>What is the cost of the one&#8211;time design fee?</p>
          </strong>
          <strong>$</strong>
          <textEntryInteraction responseIdentifier="RESPONSE21" expectedLength="10"/>
        </itemBody>
        <responseProcessing>
          <responseCondition>
            <responseIf>
              <match>
                <variable identifier="RESPONSE11"/>
                <baseValue baseType="string">3.89</baseValue>
              </match>
              <setOutcomeValue identifier="NUMCORRECT">
                <sum>
                  <variable identifier="NUMCORRECT"/>
                  <baseValue baseType="float">1</baseValue>
                </sum>
              </setOutcomeValue>
            </responseIf>
          </responseCondition>
          <responseCondition>
            <responseIf>
              <or>
                <match>
                  <variable identifier="RESPONSE21"/>
                  <baseValue baseType="string">104</baseValue>
                </match>
                <match>
                  <variable identifier="RESPONSE21"/>
                  <baseValue baseType="string">104.00</baseValue>
                </match>
              </or>
              <setOutcomeValue identifier="NUMCORRECT">
                <sum>
                  <variable identifier="NUMCORRECT"/>
                  <baseValue baseType="float">1</baseValue>
                </sum>
              </setOutcomeValue>
            </responseIf>
          </responseCondition>
          <responseCondition>
            <responseIf>
              <gt>
                <variable identifier="NUMCORRECT"/>
                <baseValue baseType="float">0</baseValue>
              </gt>
              <setOutcomeValue identifier="SCORE">
                <variable identifier="NUMCORRECT"/>
              </setOutcomeValue>
            </responseIf>
            <responseElse>
              <setOutcomeValue identifier="SCORE">
                <baseValue baseType="float">0</baseValue>
              </setOutcomeValue>
            </responseElse>
          </responseCondition>
        </responseProcessing>
      </assessmentItem>

    "convert response processing node to JS" in {
      println(toJs(qti).map(wrap).getOrElse("Oops!"))
      true === true
    }
  }

}
