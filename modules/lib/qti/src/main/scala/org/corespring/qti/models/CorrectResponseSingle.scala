package org.corespring.qti.models

import scala.xml.Node

case class CorrectResponseSingle(value: String) extends CorrectResponse {
  def isCorrect(responseValue: String): Boolean = responseValue == value
  def isValueCorrect(v: String, index: Option[Int]) = v == value
}

object CorrectResponseSingle {
  def apply(node: Node): CorrectResponseSingle = {

    if ((node \ "value").size != 1) {
      throw new RuntimeException(
        s"""Cardinality is set to single but there is not one <value> declared: ${(node \ "value").toString}""")
    } else {
      CorrectResponseSingle((node \ "value").text)
    }
  }

}