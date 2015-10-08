package org.corespring.qti.models

import scala.xml.Node

case class CorrectResponseRecord(values: Seq[Map[String, String]]) extends CorrectResponse {
  override def isCorrect(responseValue: String): Boolean = ???
  override def isValueCorrect(value: String, index: Option[Int]): Boolean = ???
}

object CorrectResponseRecord {

  def apply(node: Node) = {
    val keys = (node \ "fieldValue").map(f => (f \ "@identifier").text)
    new CorrectResponseRecord((node \ "correctResponse" \ "value").map(value => {
      keys.map(key => {
        key -> (value \ s"@$key").text
      }).toMap
    }))
  }

}