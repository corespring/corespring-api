package org.corespring.qtiToV2.kds

import scala.xml._

trait ProcessingTransformer extends JsBeautifier {

  def responseIf(node: Node)(implicit qti: Node): String = {
    node.withoutEmptyChildren.zipWithIndex.map{ case(node, i) => i match {
      case 0 => (expression(node), i)
      case _ => (responseRule(node), i)
    }}.map{ case(string, i) => i match {
      case 0 => s"if $string {"
      case _ => s" $string "
    }}.mkString + "}"
  }

  def responseRule(node: Node)(implicit qti: Node): String = {
    node.label match {
      case "setOutcomeValue" => setOutcomeValue(node)
      case _ => throw new Exception(s"Unsupported response rule: ${node.label}")
    }
  }

  def setOutcomeValue(node: Node)(implicit qti: Node) = {
    s"""${(node \ "@identifier").text} = "${term(node.withoutEmptyChildren.head).head}";"""
  }

  def expression(node: Node)(implicit qti: Node): String = {
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
    case child if (child.length < 2) => throw new Exception(s"$op expression must combine two or more expressions")
    case child => child.map(expression(_)).mkString(s" $op ")
  }

  private def term(node: Node)(implicit qti: Node): Seq[String] = node.label match {
    case "variable" => Seq((node \ "@identifier").text)
    case "correct" => {
      ((qti \ "responseDeclaration")
        .find(rd => (rd \ "@identifier").text == (node \ "@identifier").text)
        .getOrElse(throw new Exception("Did not find for a thing")) \ "correctResponse" \ "value").map(_.text)
    }
    case "baseValue" => Seq(node.text)
    case _ => throw new Exception(s"uhhh what? ${node}")
  }

  private implicit class NodeHelper(node: Node) {
    import Utility._
    def withoutEmptyChildren = trim(node).child.filter{ child => !child.isInstanceOf[Text] || !child.text.trim.isEmpty }
  }

}
