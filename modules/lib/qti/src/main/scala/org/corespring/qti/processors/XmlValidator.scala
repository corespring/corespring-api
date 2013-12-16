package org.corespring.qti.processors

import org.xml.sax.SAXParseException
import xml.{ XML, Elem }

trait XmlValidator {
  /**
   * Validate the xml string and return a validation result object
   * @param xmlString
   * @return
   */
  def validate(xmlString: String): XmlValidationResult
}

case class ExceptionMessage(message: String, lineNumber: Int = -1, columnNumber: Int = -1)

object XmlValidationResult {
  val success = XmlValidationResult()
}

case class XmlValidationResult(exceptions: Option[List[ExceptionMessage]] = None) {

  def +(that: XmlValidationResult): XmlValidationResult =
    XmlValidationResult(
      Some(this.exceptions.getOrElse(List[ExceptionMessage]()) ::: that.exceptions.getOrElse(List[ExceptionMessage]())))

  def success: Boolean = exceptions match {
    case e: Some[List[ExceptionMessage]] => false
    case None => true
  }
}

object XmlValidator extends XmlValidator {

  def validate(xmlString: String): XmlValidationResult = {
    try {
      val xml: Elem = XML.loadString(xmlString)
      XmlValidationResult.success
    } catch {
      case e: SAXParseException => {
        XmlValidationResult(Some(List(ExceptionMessage(e.getMessage, e.getLineNumber, e.getColumnNumber))))
      }
      case e: Exception => {
        XmlValidationResult(Some(List(ExceptionMessage("An unknown exception occurred"))))
      }
    }
  }
}
