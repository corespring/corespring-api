package org.corespring.qtiToV2.kds.interactions

import org.corespring.qtiToV2.interactions.InteractionTransformer
import org.corespring.qtiToV2.kds.XHTMLCleaner
import play.api.libs.json._

import scala.xml.Node

object TeacherInstructionsTransformer extends InteractionTransformer with XHTMLCleaner {

  def teacherInstructionsId(index: Int) = s"teacher-instructions-${index+1}"

  override def interactionJs(qti: Node): Map[String, JsObject] =
    qti.matching(n => (n.label == "partBlock") && ((n \ "@label").text == "teacherInstructions"))
      .zipWithIndex.map{case (teacherInstructions, index) => {
      teacherInstructionsId(index) ->
        Json.obj("value" -> teacherInstructions.convertNonXHTMLElements.map(_.child.mkString)
          .getOrElse(throw new Exception("Teacher instructions could not be converted")))
    }}.toMap

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
