package org.corespring.qti.models

import scala.xml.Node


case class CorrectResponseMultiple(value: Seq[String]) extends CorrectResponse {

  def isCorrect(responseValue: String) = {
    val responseList = responseValue.split(",").toList
    value.sortWith(_ < _) == responseList.sortWith(_ < _)
  }

  override def isPartOfCorrect(responseValue: String): Boolean = {
    val responseList = responseValue.split(",").toList
    responseList.foldLeft(true)((acc, r) => acc && value.contains(r))
  }

  def isValueCorrect(v: String, index: Option[Int]) = value.contains(v)

}

object CorrectResponseMultiple {
  def apply(node: Node): CorrectResponseMultiple = CorrectResponseMultiple((node \ "value").map(_.text))
}