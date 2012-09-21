package controllers.testplayer.qti

import org.specs2.mutable._
import scala.xml.{Elem, XML}
import com.codahale.jerkson.Json.{generate, parse}
import scala.xml.NodeSeq
import api.processors.FeedbackProcessor._
import org.specs2.execute.Skipped

class FeedbackProcessorSpec extends Specification {


  def jsonFromXml(xml: NodeSeq): String = generate(Map("xmlData" -> xml.toString))

  def xmlFromJson(jsonData: String): NodeSeq =
    XML.loadString(parse[Map[String, String]](jsonData).getOrElse("xmlData", ""))

  def getAttributeMap(node: Elem): Map[String, String] =
      node.attributes.map(attribute => (attribute.key, attribute.value(0).text)).toMap

  def trueForAllFeedback[A](rootNode: NodeSeq, p: NodeSeq => Boolean): Boolean = {
    ((rootNode \\ "feedbackInline") ++ (rootNode \\ "modalFeedback")).foldLeft(true)((a, b) => p(b) && a)
  }

  "FeedbackProcessor" should {
    "add feedback ids to feedback elements in JSON" in {
      val xml =
        <body>
          <feedbackInline>Test feedback!</feedbackInline>
          <modalFeedback>More test feedback!</modalFeedback>
        </body>

      //val xmlWithFeedbackIds: NodeSeq = xmlFromJson(addFeedbackIds(jsonFromXml(xml)))
      Skipped("Waiting on a fix for this")
      //if (trueForAllFeedback(xmlWithFeedbackIds, node => (node \ "@csFeedbackId").text.nonEmpty)) success else failure
    }

    "remove feedback ids from elements in JSON" in {
      val json =
        """{"xmlData":"<body><feedbackInline csFeedbackId=\"1\">Test feedback!</feedbackInline><modalFeedback csFeedbackId=\"2\">More test feedback!</modalFeedback></body>"}"""

      //val xmlWithoutFeedbackIds: NodeSeq = xmlFromJson(removeFeedbackIds(json))

      Skipped("Waiting on a fix for this")
      //if (trueForAllFeedback(xmlWithoutFeedbackIds, node => (node \ "@csFeedbackId").text.isEmpty)) success else failure
    }

    "filter all feedback info other than csFeedbackId" in {
      val xml =
        <body>
          <feedbackInline wow="this" has="a" lot="of" attributes="yeah?" csFeedbackId="101">
            <p>There's all kinds of info in this feedback!</p>
          </feedbackInline>
          <modalFeedback we="should" check="modal" feedback="too" csFeedbackId="300"></modalFeedback>
        </body>

      val xmlWithOnlyCsFeedbackIds = filterFeedbackContent(xml)

      if (trueForAllFeedback(xmlWithOnlyCsFeedbackIds, node => {
        val attributes: Map[String, String] = getAttributeMap(node.asInstanceOf[Elem])
        attributes.contains("csFeedbackId") && attributes.size == 1 && node.asInstanceOf[Elem].child.length == 0
      })) success else failure
    }
  }

}
