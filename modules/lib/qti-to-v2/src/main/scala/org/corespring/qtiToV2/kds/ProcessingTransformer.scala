package org.corespring.qtiToV2.kds

import scala.xml._

trait ProcessingTransformer extends JsBeautifier {

  def toJs(node: Node)(implicit qti: Node) = node.label match {
    case "responseProcessing" =>
      (outcomeDeclarations(qti) ++ node.withoutEmptyChildren.map(responseCondition)).mkString("\n")
    case _ => throw ProcessingTransformerException("Cannot process node $label as responseProcessing", node)
  }

  protected def outcomeDeclarations(qti: Node) = {
    def default[T](node: Node, _def: Option[T], fn: (String => String) = t => t.toString): String = {
      (node \ "value").length match {
        case 0 => _def match {
          case Some(default) => fn(default.toString)
          case _ => throw ProcessingTransformerException(s"Cannot have unassigned default for: $node", node)
        }
        case 1 => fn((node \ "value").text)
        case _ => throw ProcessingTransformerException(
          "Cannot have multiple default values for outcomeDeclaration: $node", node)
      }
    }
    (qti \\ "outcomeDeclaration").map { outcomeDeclaration =>
      val defaultValue = (outcomeDeclaration \ "@baseType").text match {
        case "string" => default[String](outcomeDeclaration, Some(""), t => s""""$t"""")
        case "float" => default[Float](outcomeDeclaration, Some(0.0f))
        case "integer" => default[Int](outcomeDeclaration, Some(0))
        case "identifier" => default[String](outcomeDeclaration, None)
        case other: String => throw ProcessingTransformerException(
          "Cannot parse outcomeDeclaration of type " + other + ": $node", outcomeDeclaration)
      }
      s"var ${(outcomeDeclaration \ "@identifier")} = $defaultValue;"
    }
  }

  protected def responseCondition(node: Node)(implicit qti: Node) =
    node.withoutEmptyChildren.map(child => child.label match {
      case "responseIf" => responseIf(child)
      case "responseElse" => responseElse(child)
      case "responseElseIf" => responseElseIf(child)
      case _ => throw ProcessingTransformerException("Not a supported conditional statement: $label", child)
    }).mkString

  protected def responseIf(node: Node)(implicit qti: Node) =
    conditionalStatement(node, "if $string {", " $string ")

  private def responseElseIf(node: Node)(implicit qti: Node) =
    conditionalStatement(node, " else if $string {", " $string ")

  private def responseElse(node: Node)(implicit qti: Node) =
    s" else { ${node.withoutEmptyChildren.map(responseRule).mkString(" ")} }"

  private def conditionalStatement(node: Node, expressionWrapper: String, responseWrapper: String)(implicit qti: Node) =
    node.withoutEmptyChildren.zipWithIndex.map{ case(node, i) => i match {
      case 0 => (expression(node), i)
      case _ => (responseRule(node), i)
    }}.map{ case(string, i) => i match {
      case 0 => expressionWrapper.replace("$string", string)
      case _ => responseWrapper.replace("$string", string)
    }}.mkString + "}"

  protected def responseRule(node: Node)(implicit qti: Node) = node.label match {
    case "setOutcomeValue" => setOutcomeValue(node)
    case _ => throw new Exception(s"Unsupported response rule: ${node.label}")
  }

  protected def setOutcomeValue(node: Node)(implicit qti: Node) =
    s"""${(node \ "@identifier").text} = ${expression(node.withoutEmptyChildren.head)};"""

  protected def expression(node: Node)(implicit qti: Node): String = node.label match {
    case "match" => s"(${_match(node)})"
    case "and" => s"(${and(node)})"
    case "or" => s"(${or(node)})"
    case "gt" => s"(${gt(node)})"
    case "sum" => sum(node)
    case "variable" => (node \ "@identifier").text
    case "correct" => correct(node)
    case "baseValue" => baseValue(node)
    case _ => throw new Exception(s"Not a supported expression: ${node.label}")
  }

  protected def gt(node: Node)(implicit qti: Node) = binaryOp(node, ">")
  protected def gte(node: Node)(implicit qti: Node) = binaryOp(node, ">=")
  protected def lt(node: Node)(implicit qti: Node) = binaryOp(node, "<")
  protected def lte(node: Node)(implicit qti: Node) = binaryOp(node, "<=")

  protected def correct(node: Node)(implicit qti: Node) = {
    val rd = (qti \ "responseDeclaration").find(rd => (rd \ "@identifier").text == (node \ "@identifier").text)
      .getOrElse(throw new Exception("Did not find for a thing"))
    (rd \ "correctResponse" \ "value").map(_.text).map(v => {
      (rd \ "@baseType").text match {
        case "string" => s""""$v""""
        case _ => v
      }
    }).head
  }

  protected def baseValue(node: Node) = {
    (node \ "@baseType").text match {
      case "string" => s""""${node.text}""""
      case _ => node.text
    }
  }

  protected def sum(node: Node)(implicit qti: Node) = node.withoutEmptyChildren.map(expression).mkString(" + ")

  protected def _match(node: Node)(implicit qti: Node) = {
    node.withoutEmptyChildren match {
      case Seq(lhs, rhs) => s"""${expression(lhs)} === ${expression(rhs)}"""
      case e: Seq[String] =>
        throw new Exception(s"Match can only have two children in ${node.withoutEmptyChildren}")
    }
  }

  protected def and(node: Node)(implicit qti: Node) = binaryOp(node, "&&")
  protected def or(node: Node)(implicit qti: Node) = binaryOp(node, "||")

  private def binaryOp(node: Node, op: String)(implicit qti: Node): String = node.withoutEmptyChildren match {
    case child if (child.length < 2) => throw new Exception(s"$op expression must combine two or more expressions")
    case child => child.map(expression(_)).mkString(s" $op ")
  }

  private implicit class NodeHelper(node: Node) {
    import Utility._
    def withoutEmptyChildren = trim(node).child.filter{ child => !child.isInstanceOf[Text] || !child.text.trim.isEmpty }
  }

  private case class ProcessingTransformerException(message: String, node: Node) extends Exception {
    override def getMessage = message.replace("$label", node.label).replace("$node", node.toString)
  }

}
