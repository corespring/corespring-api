package org.corespring.qti.models

import scala.xml.Node

case class CorrectResponseOrdered(value: Seq[String]) extends CorrectResponse {
  def isCorrect(responseValue: String) = {
    val responseList = responseValue.split(",").toList
    value == responseList
  }

  def isValueCorrect(v: String, index: Option[Int]) = {
    index match {
      case Some(i) => value.length > i && value(i) == v
      case None => false
    }
  }
}

object CorrectResponseOrdered {
  def apply(node: Node): CorrectResponseOrdered = CorrectResponseOrdered((node \ "value").map(_.text))
}