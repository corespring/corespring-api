package org.corespring.qtiToV2.kds.interactions

import org.corespring.qtiToV2.interactions.InteractionTransformer
import org.corespring.qtiToV2.kds.XHTMLCleaner
import play.api.libs.json._

import scala.xml.Node

object TeacherInstructionsTransformer extends InteractionTransformer with XHTMLCleaner {

  val isInstructions: Node => Boolean = n =>
    ((n.label == "partBlock") && Seq("@label", "@identifier").find(a => (n \ a).text == "teacherInstructions").nonEmpty) ||
      (n.label == "teacherInstructions")

  def teacherInstructionsId(node: Node) = s"teacher-instructions-${node.hashCode()}"

  override def transform(node: Node, manifest: Node) = node match {
    case node: Node if (isInstructions(node)) =>
      <corespring-teacher-instructions id={ teacherInstructionsId(node) }></corespring-teacher-instructions>
    case _ => node
  }

  override def interactionJs(qti: Node, manifest: Node): Map[String, JsObject] =
    qti.matching(isInstructions)
      .zipWithIndex.map {
        case (teacherInstructions, index) => {
          teacherInstructionsId(teacherInstructions) ->
            Json.obj(
              "componentType" -> "corespring-teacher-instructions",
              "value" -> teacherInstructions.convertNonXHTMLElements.map(_.child.mkString)
                .getOrElse(throw new Exception("Teacher instructions could not be converted")))
        }
      }.toMap

  /**
   * Decorator for Node which adds a method to return all nodes matching a predicate function.
   */
  implicit class NodeWithFinder(node: Node) {

    def matching(predicate: Node => Boolean) = recurse(node, predicate)

    private def recurse(node: Node, predicate: Node => Boolean, matches: Seq[Node] = Seq.empty): Seq[Node] =
      (predicate(node), node.child.nonEmpty) match {
        case (true, true) => matches ++ node ++ node.child.map(recurse(_, predicate)).flatten
        case (false, true) => matches ++ node.child.map(recurse(_, predicate)).flatten
        case (true, false) => Seq(node)
        case (false, false) => Seq.empty
      }

  }

}
