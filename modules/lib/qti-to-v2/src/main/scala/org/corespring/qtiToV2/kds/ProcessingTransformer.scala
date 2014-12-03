package org.corespring.qtiToV2.kds

import scala.xml.{Text, Node}

trait ProcessingTransformer {

  def expression(node: Node, _qti: Node): String = {
    implicit val qti = _qti
    s"(${node.label match {
      case "match" => _match(node)
      case "and" => and(node)
      case _ => throw new Exception(s"Not a supported expression: ${node.label}")
    }})"
  }

  def _match(node: Node)(implicit qti: Node) = {
    node.withoutEmptyChildren match {
      case Seq(lhs, rhs) => {
        val lh = term(lhs)
        val rh = term(rhs)
        (lh.length, rh.length) match {
          case (1, 1) => s"""${lh.head} == "${rh.head}""""
          case (1, _) => s"""["${rh.mkString("\",\"")}"].indexOf(${lh.head}) >= 0"""
          case _ => throw new Exception("Too many parameters on left hand side")
        }
      }
      case e: Seq[String] =>
        throw new Exception(s"Match can only have two children in ${node.withoutEmptyChildren}")
    }
  }

  def and(node: Node)(implicit qti: Node) = binaryOp(node, "&&")
  def or(node: Node)(implicit qti: Node) = binaryOp(node, "||")

  private def binaryOp(node: Node, op: String)(implicit qti: Node) = node.withoutEmptyChildren match {
    case child if (child.length < 2) => throw new Exception("And expression must combine two or more expressions")
    case child => child.map(child => expression(child, qti)).mkString(s" $op ")
  }

  private def term(node: Node)(implicit qti: Node): Seq[String] = node.label match {
    case "variable" => Seq((node \ "@identifier").text.toString)
    case "correct" => {
      ((qti \ "responseDeclaration")
        .find(rd => (rd \ "@identifier").text == (node \ "@identifier").text)
        .getOrElse(throw new Exception("Did not find for a thing")) \ "correctResponse" \ "value").map(_.text)
    }
  }

  private implicit class NodeHelper(node: Node) {
    def withoutEmptyChildren = node.child.filter{ child => !child.isInstanceOf[Text] || !child.text.trim.isEmpty }
  }

}
