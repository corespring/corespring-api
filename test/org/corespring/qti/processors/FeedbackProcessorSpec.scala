package org.corespring.qti.processors

import org.corespring.platform.core.models.itemSession.FeedbackIdMapEntry
import org.specs2.mutable._
import play.api.libs.json.{JsObject, JsString, Json}
import scala.xml.{Node, Elem, XML, NodeSeq}

class FeedbackProcessorSpec extends Specification {


  def jsonFromXml(xml: NodeSeq): String = Json.stringify(JsObject(Seq("xmlData" -> JsString(xml.toString))))

  def xmlFromJson(jsonData: String): NodeSeq ={
    val json = Json.parse(jsonData)
    val map : Map[String,String] = json.as[Map[String,String]]
    XML.loadString(map.getOrElse("xmlData", ""))
  }

  def getAttributeMap(node: Elem): Map[String, String] =
      node.attributes.map(attribute => (attribute.key, attribute.value(0).text)).toMap

  def trueForAllFeedback[A](rootNode: NodeSeq, p: NodeSeq => Boolean): Boolean = {
    ((rootNode \\ "feedbackInline") ++ (rootNode \\ "modalFeedback")).foldLeft(true)((a, b) => p(b) && a)
  }

  def identifierEquals(value: String)(node: Node) = {
    (node \ "@identifier" ).text == value
  }

  def nodeWithFeedbackId(xml:Node, id:String) : Node = {
     (xml \\ "_" ).filter( identifierEquals(id)).head
  }

  "FeedbackProcessor" should {
    "add feedback ids to xml and create map of feedbackIds -> identifiers" in {
      val xml =
        <body>
          <feedbackInline identifier="testFeedback">Test feedback!</feedbackInline>
          <modalFeedback identifier="moreTestFeedback">More test feedback!</modalFeedback>
        </body>

      val expectedMap = Seq(FeedbackIdMapEntry("1","","testFeedback"), FeedbackIdMapEntry("2","","moreTestFeedback"))

      val expectedXml =
        <body>
          <feedbackInline csFeedbackId="1" identifier="testFeedback">Test feedback!</feedbackInline>
          <modalFeedback csFeedbackId="2" identifier="moreTestFeedback">More test feedback!</modalFeedback>
        </body>

      val (xmlWithIds, feedbackIdToIdentifierMap) = FeedbackProcessor.addFeedbackIds(xml)

      feedbackIdToIdentifierMap mustEqual expectedMap
      xmlWithIds mustEqual expectedXml
    }

    "add the csFeedbackId to the node that is declared in the map" in {
      val elem = <feedbackInline identifier="test1"></feedbackInline>
      val map = Seq(FeedbackIdMapEntry("12345","","test1"))
      val result = FeedbackProcessor.addFeedbackIds(elem,map)
      (result \ "@csFeedbackId").text must equalTo("12345")
    }


    "filter all feedback info other than csFeedbackId" in {
      val xml =
        <body>
          <feedbackInline wow="this" has="a" lot="of" attributes="yeah?" csFeedbackId="101">
            <p>There's all kinds of info in this feedback!</p>
          </feedbackInline>
          <modalFeedback we="should" check="modal" feedback="too" csFeedbackId="300"></modalFeedback>
        </body>

      val xmlWithOnlyCsFeedbackIds = FeedbackProcessor.filterFeedbackContent(xml)

      println(xmlWithOnlyCsFeedbackIds)

      if (trueForAllFeedback(xmlWithOnlyCsFeedbackIds, node => {
        val attributes: Map[String, String] = getAttributeMap(node.asInstanceOf[Elem])
        attributes.contains("csFeedbackId") && node.asInstanceOf[Elem].child.length == 0
      })) success else failure
    }
  }

}
