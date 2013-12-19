package org.corespring.qti.models

import org.corespring.common.log.PackageLogging
import scala.xml.Node

case class CorrectResponseTargeted(value: Map[String, List[String]],
                                   orderedTargets: Set[String] = Set()) extends CorrectResponse with PackageLogging {

  def isCorrect(responseValue: String) = {
    val responseList = responseValue.split(",").toList
    val responseMap =
      responseList.map {
        el =>
          val target = el.split(":")(0)
          val answersArray = el.split(":")(1).split("\\|")
          val positionMatters = orderedTargets.contains(target)
          target -> (if (positionMatters) answersArray.toList else answersArray.toSet)
      }.toMap

    val valueMap = value.map { it =>
      val positionMatters = orderedTargets.contains(it._1)
      it._1 -> (if (positionMatters) it._2.toList else it._2.toSet)
    }.toMap

    responseMap == valueMap
  }

  def isValueCorrect(v: String, index: Option[Int]) = {
    val parts = v.split(":")
    val target = parts(0)
    val positionMatters = orderedTargets.contains(target)
    val answerList = if (positionMatters) parts(1).split("\\|").toList else parts(1).split("\\|").toSet
    val correctList = if (positionMatters) value(target) else value(target).toSet
    answerList == correctList
  }

}

object CorrectResponseTargeted {
  def apply(node: Node, orderedTargets: Set[String]): CorrectResponseTargeted = {
    CorrectResponseTargeted(
      (node \ "value").map(e => e.attribute("identifier").get.text -> (e \ "value").map(_.text).toList).toMap, orderedTargets)
  }
}

