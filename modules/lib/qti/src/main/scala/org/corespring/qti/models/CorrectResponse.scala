package org.corespring.qti.models

import scala.xml.Node
import org.corespring.qti.models.interactions.{DragAndDropInteraction, SelectTextInteraction, TextEntryInteraction, Interaction}

trait CorrectResponse {
  def isCorrect(responseValue: String): Boolean
  def isPartOfCorrect(responseValue: String): Boolean = isCorrect(responseValue)
  def isValueCorrect(value: String, index: Option[Int]): Boolean
}

object CorrectResponse {

  /**
   * Note: TextEntryInteractions are a special case. If cardinality is equation, use CorrectResponseLineEquation, otherwise,
   * no matter what the cardinality is we treat their responses as CorrectResponseAny
   * @param node
   * @param cardinality
   * @param interaction
   * @return
   */
  def apply(node: Node, cardinality: String, baseType: String, interaction: Option[Interaction] = None): CorrectResponse = {
    if (interaction.isDefined) {
      interaction.get match {
        case TextEntryInteraction(_, _, _) => baseType match {
          case line if line.startsWith("line") => CorrectResponseEquation(node, line)
          case eqn if eqn.startsWith("eqn") => CorrectResponseEquation(node, eqn)
          case _ => CorrectResponseAny(node)
        }
        case SelectTextInteraction(_, _, _, _, _, _) => CorrectResponseAny(node)
        case DragAndDropInteraction(_, _, targets) =>
          CorrectResponseTargeted(node, targets.filter(_.cardinality == "ordered").map(_.identifier).toSet)
        case _ => CorrectResponse(node, cardinality, baseType)
      }
    } else CorrectResponse(node, cardinality, baseType)
  }

  def apply(node: Node, cardinality: String, baseType: String): CorrectResponse = {
    cardinality match {
      case "single" => baseType match {
        case line if line.startsWith("line") => CorrectResponseEquation(node, line)
        case eqn if eqn.startsWith("eqn") => CorrectResponseEquation(node, eqn)
        case _ => CorrectResponseSingle(node)
      }
      case "multiple" => CorrectResponseMultiple(node)
      case "ordered" => CorrectResponseOrdered(node)
      case "targeted" => CorrectResponseTargeted(node, Set[String]())
      case "record" => CorrectResponseRecord(node)
      case _ => throw new RuntimeException(
        s"""unknown cardinality: ${cardinality}. cannot generate CorrectResponse""")
    }
  }

}