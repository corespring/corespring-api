package org.corespring.qtiToV2.interactions

import scala.xml._

case class NumberedLinesTransformer(qti: Node) extends InteractionTransformer {

  override def transform(node: Node): Seq[Node] = node
  override def interactionJs(qti: Node) = NumberedLinesTransformer.interactionJs(qti)

}

object NumberedLinesTransformer extends Transformer {

  def interactionJs(qti: Node) =
    (qti \\ "_").filter(isNumberedLinesNode(_)).zipWithIndex.map {
      case (node, index) => {
        id(index + 1) -> Json.obj(
          "componentType" -> "corespring-numbered-lines",
          "weight" -> 0,
          "model" -> Json.obj(
            "lines" -> (node \\ "line").map(_.child.mkString)))
      }
    }.toMap

  override def transform(qti: Node): Node = {
    var count: Int = 0

    def recurse(node: Node): Seq[Node] = node match {
      case node: Node if isNumberedLinesNode(node) => {
        count = count + 1
        <corespring-numbered-lines id={ id(count) }></corespring-numbered-lines>
      }
      case e: Elem => e.copy(child = e.nonEmptyChildren.map(recurse(_).headOption).flatten)
      case _ => node
    }
    recurse(qti).head
  }

  private def isNumberedLinesNode(node: Node) = (node \ "@class").text.contains("numbered-lines")
  private def id(index: Int) = s"numbered_lines_${index}"

}
