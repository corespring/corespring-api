package org.corespring.qtiToV2.kds

import org.specs2.mutable.Specification

class ResponseProcessingTemplateConverterTest extends Specification {

  val mockXMLResponse =
    <responseProcessing>
      <xml></xml>
    </responseProcessing>

  val mockWS = (test: String) => mockXMLResponse

  val converter = new ResponseProcessingTemplateConverter(mockWS)
  import converter._

  "hasTemplate" should {

    "when node has no template" should {
      val node = <responseProcessing></responseProcessing>

      "return false" in {
        node.hasTemplate must beFalse
      }

    }

    "when node has template" should {
      val node = <responseProcessing template="hey I'm a template!"></responseProcessing>

      "return true" in {
        node.hasTemplate must beTrue
      }
    }

  }


  "withTemplate" should {

    "when node has no template" should {
      val node = <responseProcessing></responseProcessing>

      "return node" in {
        node.withTemplate must beEqualTo(node)
      }

    }

    "when node has template" should {
      val node = <responseProcessing template="http://www.imsglobal.org/question/qti_v2p1/rptemplates/match_correct"></responseProcessing>

      "return node with template from url" in {
        node.withTemplate must beEqualTo(mockXMLResponse)
      }
    }

  }

  "substituting" should {

    val originalIdentifier = "RESPONSE"
    val newIdentifier = "RESPONSE1"

    def processingFor(identifier: String) =
      <responseProcessing>
        <responseCondition>
          <responseIf>
            <match>
              <variable identifier={identifier}/>
              <correct identifier={identifier}/>
            </match>
            <setOutcomeValue identifier="SCORE">
              <baseValue baseType="float">1</baseValue>
            </setOutcomeValue>
          </responseIf>
          <responseElse>
            <setOutcomeValue identifier="SCORE">
              <baseValue baseType="float">0</baseValue>
            </setOutcomeValue>
          </responseElse>
        </responseCondition>
      </responseProcessing>

    "replace one identifier value with another" in {
      processingFor(originalIdentifier).substituting(originalIdentifier -> newIdentifier) must beEqualTo(processingFor(newIdentifier))
    }

  }

}
