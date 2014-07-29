package org.corespring.qti.models

import scala.xml.Node

case class CorrectResponseAny(value: Seq[String]) extends CorrectResponse {

  def isCorrect(responseValue: String) = value.find(_ == responseValue).isDefined

  def isValueCorrect(v: String, index: Option[Int]) = value.contains(v)

}

object CorrectResponseAny {

  def apply(node: Node): CorrectResponseAny = CorrectResponseAny((node \ "value").map(_.text))

}
