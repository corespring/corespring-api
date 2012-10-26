package tests.models

import org.specs2.mutable.Specification
import models.{SessionData, ItemResponse}
import models.SessionData.SessionDataWrites
import scala.Some
import play.api.Play
import io.Codec
import java.nio.charset.Charset
import xml.XML
import qti.models.QtiItem

class SessionDataTest extends Specification {

  "SessionData" should {

    "return correct feedback for inline item" in {
      val XML =
        <assessmentItem>
          <correctResponseFeedback>Looking good buddy</correctResponseFeedback>
          <incorrectResponseFeedback>You should rethink this</incorrectResponseFeedback>
          <responseDeclaration identifier="manOnMoon" cardinality="single" baseType="identifier">
            <correctResponse>
              <value>armstrong</value>
            </correctResponse>
          </responseDeclaration>
          <itemBody>
            <inlineChoiceInteraction
            responseIdentifier="manOnMoon"
            required="false">
              <inlineChoice identifier="armstrong">Neil Armstrong
                <feedbackInline csFeedbackId="1" identifier="armstrong" defaultFeedback="true"/>
              </inlineChoice>
              <inlineChoice identifier="aldrin">Buzz Aldrin
                <feedbackInline csFeedbackId="2" identifier="aldrin" defaultFeedback="true"/>
              </inlineChoice>
            </inlineChoiceInteraction>
          </itemBody>
        </assessmentItem>

      val qtiItem = QtiItem(XML)

      val correctResponse = ItemResponse("manOnMoon", "armstrong")
      val incorrectResponse = ItemResponse("manOnMoon", "aldrin")

      val correctJson = SessionDataWrites.writes( SessionData(qtiItem, Seq(correctResponse)) )
      (correctJson \ "feedbackContents" \ "1").asOpt[String] must beSome((XML\\ "correctResponseFeedback").text)

      val incorrectJson = SessionDataWrites.writes( SessionData(qtiItem, Seq(incorrectResponse)))
      (incorrectJson \ "feedbackContents" \ "2").asOpt[String] must beSome((XML\\ "incorrectResponseFeedback").text)
    }

    "return correct feedback from all items" in {

      val path = "test/mockXml/all-items.xml"
      val s = io.Source.fromFile(path)(new Codec(Charset.forName("UTF-8"))).mkString
      val xml = XML.loadString(s)

      val qtiItem = QtiItem(xml)

      val correctResponse = ItemResponse("manOnMoon", "armstrong")
      val incorrectResponse = ItemResponse("manOnMoon", "aldrin")

      val correctJson = SessionDataWrites.writes( SessionData( qtiItem, Seq(correctResponse)))
      println(correctJson)
      (correctJson \ "feedbackContents" \ "armstrong").asOpt[String] must beSome( (xml \\ "correctResponseFeedback").text)
    }
  }
}
