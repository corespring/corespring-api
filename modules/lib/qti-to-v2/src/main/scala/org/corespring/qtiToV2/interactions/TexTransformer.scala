package org.corespring.qtiToV2.interactions

import scala.xml.{ Elem, Node, Text }

import play.api.libs.json._
object TexTransformer extends InteractionTransformer {

  implicit class TexConverter(child: Seq[Node]) {

    def convertToTex(implicit parent: Node): Node = Text((parent \ "@inline").text match {
      case "false" => s"$$$$${child.map(clearNamespace).mkString}$$$$"
      case _ => s"\\(${child.map(clearNamespace).mkString}\\)"
    })

  }

  override def transform(node: Node) = {
    implicit val parent = node
    node match {
      case elem: Elem if elem.label == "tex" => elem.child.convertToTex
      case _ => node
    }
  }

  override def interactionJs(qti: Node) = Map.empty[String, JsObject]

}
