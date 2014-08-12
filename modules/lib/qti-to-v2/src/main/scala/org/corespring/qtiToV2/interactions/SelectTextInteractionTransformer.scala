package org.corespring.qtiToV2.interactions

import scala.xml._

import play.api.libs.json._
import scala.util.matching.Regex
import org.corespring.qti.models.interactions.SelectTextInteraction

object SelectTextInteractionTransformer extends InteractionTransformer {

  object Defaults {
    val shuffle = false
  }

  override def interactionJs(qti: Node): Map[String, JsObject] = (qti \\ "selectTextInteraction").map(implicit node => {
    (node \ "@responseIdentifier").text ->
      Json.obj(
        "componentType" -> "corespring-select-text",
        "model" -> Json.obj(
          "choices" -> choices,
          "config" -> partialObj(
            "selectionUnit" -> optForAttr[JsString]("selectionType"),
            "checkIfCorrect" -> optForAttr[JsString]("checkIfCorrect"),
            "minSelections" -> optForAttr[JsNumber]("minSelections"),
            "maxSelections" -> optForAttr[JsNumber]("maxSelections"),
            "showFeedback" -> Some(JsBoolean(false)))))
  }).toMap

  override def transform(node: Node): Seq[Node] = node match {
    case elem: Elem if elem.label == "selectTextInteraction" => {
      val identifier = (elem \ "@responseIdentifier").text
      <corespring-select-text id={ identifier }></corespring-select-text>
    }
    case _ => node
  }

  private def choices(implicit node: Node): JsArray = {
    def isCorrect(string: String) = {
      XML.loadString("<div>" + string + "</div>") \ "correct" match {
        case empty: NodeSeq if empty.length == 0 => false
        case _ => true
      }
    }
    def stripCorrectness(string: String) =
      string.replaceAll("<[/]*correct>","")

    val text = clearNamespace(node.child).mkString
    val choices = optForAttr[JsString]("selectionType") match {
      case Some(selection) if selection.equals(JsString("word")) => TextSplitter.words(text)
      case _ => TextSplitter.sentences(text)
    }

    JsArray(choices.map(choice => partialObj(
      "data" -> Some(JsString(stripCorrectness(choice))),
      "correct" -> (if (isCorrect(choice)) Some(JsBoolean(true)) else None))))
  }


}

object TextSplitter {

  def sentences(s: String): Seq[String] = {
    val regExp = new Regex("(?s)(.*?[.!?]([^ \\t])*)", "match")
    // Filter out names like Vikram S. Pandit as they break the sentence parsing
    val namesParsed = "([A-Z][a-z]+ [A-Z])\\.( [A-Z][a-z]+)".r.replaceAllIn(s, "$1&#46;$2")
    regExp.findAllMatchIn(namesParsed).map({m => m.group("match").trim }).toList
  }

  def words(s: String): Seq[String] = {
    def getWordsWithXmlTags(node: Node, path: Seq[Node]):Seq[String] = {
      if (node.isInstanceOf[Text]) {
        val l = path.map(_.label).dropRight(1).reverse
        def textWithNeighbouringTags(left:Seq[String], res:String):String = {
          if (left.isEmpty) {
            res
          } else {
            val elem = left.head
            s"<${elem}>${textWithNeighbouringTags(left.tail, res)}</${elem}>"
          }
        }

        val textWithTags = textWithNeighbouringTags(l, node.text.trim)
        s"\\S+".r.findAllIn(textWithTags).toList
      } else {
        node.child.map { n => getWordsWithXmlTags(n, Seq(node) ++ path) }.flatten
      }
    }
    val inputXml = XML.loadString(s"<span>${s}</span>")
    getWordsWithXmlTags(inputXml, Seq())
  }

}