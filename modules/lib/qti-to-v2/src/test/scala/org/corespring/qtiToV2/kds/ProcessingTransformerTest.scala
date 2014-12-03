package org.corespring.qtiToV2.kds

import org.specs2.mutable.Specification

import scala.xml.Node

class ProcessingTransformerTest extends Specification {

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
      ProcessingTransformer._match(matchNode)(node) must be equalTo(s"""$responseId == "${correctResponses.head}"""")
    }

    "translate equivalence of variable to multiple correct values" in {
      val correctResponses = Seq("a", "b")
      val node = qti(correctResponses = correctResponses)
      val matchNode = matcher(node)
      ProcessingTransformer._match(matchNode)(node) must be equalTo(s"""["${correctResponses.mkString("\",\"")}"].indexOf($responseId) >= 0""")
    }

  }

}
