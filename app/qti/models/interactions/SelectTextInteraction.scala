package qti.models.interactions

import xml.{XML, NodeSeq, Elem, Node}
import xml.transform.{RuleTransformer, RewriteRule}
import util.matching.Regex
import qti.models._
import models.{ItemResponseOutcome, ItemResponse}
import testplayer.views.utils.QtiScriptLoader
import controllers.Log
import scala.Some
import models.ArrayItemResponse

case class SelectTextInteraction(responseIdentifier: String, selectionType: String, minSelection: Int, maxSelection: Int, correctResponse: Option[CorrectResponse], responseDeclaration: Option[ResponseDeclaration]) extends Interaction {

  override def validate(qtiItem: QtiItem) = {
    (true, "Ok")
  }

  def getChoice(identifier: String) = None

  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: ItemResponse): Option[ItemResponseOutcome] = {
    var score:Float = 0;
    var outcomeProperties:Map[String,Boolean] = Map();
    response match {
      case ArrayItemResponse(_, responseValue, _) => correctResponse match {
        case Some(cr) => {
          if (cr.isCorrect(response.value)) score = 1
          if (responseValue.size > maxSelection){
            outcomeProperties = outcomeProperties + ("responsesExceedMax" -> true)
          }else{
            outcomeProperties = outcomeProperties + ("responsesExceedMax" -> false)
          }
          if (responseValue.size < minSelection) {
            outcomeProperties = outcomeProperties + ("responsesBelowMin" -> true)
          }else{
            outcomeProperties = outcomeProperties + ("responsesBelowMin" -> false)
          }
          Some(ItemResponseOutcome(score,None,outcomeProperties))
        }
        case _ => None
      }
      case _ => {
        Log.e("received a response that was not an array response in SelectTextInteraction.getOutcome")
        None
      }
    }
  }

  override def getResponseDeclaration: Option[ResponseDeclaration] = {
    responseDeclaration
  }

}

object SelectTextInteraction extends InteractionCompanion[SelectTextInteraction] {
  def apply(node: Node, itemBody: Option[Node]): SelectTextInteraction = {

    val correctAnswers = Some(CorrectResponseMultiple(SelectTextInteraction.parseCorrectResponses(node)))
    val id = (node \ "@responseIdentifier").text
    val responseDeclaration = Some(ResponseDeclaration(identifier = id, cardinality = "multiple", correctResponse = correctAnswers, mapping = None))

    SelectTextInteraction(
      (node \ "@responseIdentifier").text,
      (node \ "@selectionType").text,
      (node \ "@minSelections").text.toInt,
      (node \ "@maxSelections").text.toInt,
      correctAnswers,
      responseDeclaration
    )
  }

  def parse(itemBody: Node): Seq[Interaction] = {
    val interactions = (itemBody \\ "selectTextInteraction")
    if (interactions.isEmpty) {
      Seq()
    } else {
      interactions.map(node => SelectTextInteraction(node, Some(itemBody)))
    }
  }

  def interactionMatch(e: Elem): Boolean = e.label == "selectTextInteraction"

  override def preProcessXml(interactionXml: Elem): NodeSeq = tokenizeSelectText(interactionXml)

  def parseCorrectResponses(selectnodeXml: NodeSeq): Seq[String] = {
    val isWord = (selectnodeXml \ "@selectionType").text == "word"
    val taggedXml = if (isWord) performOnText(selectnodeXml, tagWords) else performOnText(selectnodeXml, tagSentences)
    val idRegexp = new Regex("id=\".([0-9]+)", "match")
    val correctIndexes =
      if (isWord) {
        (taggedXml \ "correct").map(n => idRegexp.findFirstMatchIn(n.mkString).get.group("match"))
      } else {
        (taggedXml
          \ "span"
          filterNot (_ \ "@selectable" isEmpty)
          filterNot (_ \ "correct" isEmpty)
          ).map(n => idRegexp.findFirstMatchIn(n.mkString).get.group("match"))
      }
    correctIndexes
  }

  private def tokenizeSelectText(interactionXml: NodeSeq): NodeSeq = {
    val isWord = (interactionXml \ "@selectionType").text == "word"
    val taggedXml = if (isWord) performOnText(interactionXml, tagWords) else performOnText(interactionXml, tagSentences)
    new RuleTransformer(RemoveCorrectTags).transform(taggedXml)
  }

  private def performOnText(e: NodeSeq, fn: String => String): NodeSeq = {
    val xmlText = e.mkString
    val regExp = "<.*?>".r
    val matches = regExp.findAllIn(xmlText).toList

    val openString = matches.head
    val closeString = regExp.findAllIn(xmlText).toList.last

    val lastIndex = xmlText.lastIndexOf("<")
    val resultText = xmlText.substring(0, lastIndex).replaceFirst("<.*?>", "")

    val transformedText = fn(resultText)

    XML.loadString(openString + transformedText + closeString)
  }

  def tagSentences(s: String): String = {
    val regExp = new Regex("(?s)(.*?[.!?](<\\/correct>)*)", "match")
    var idx = 0

    // Filter out names like Vikram S. Pandit as they break the sentence parsing
    val namesParsed = "([A-Z][a-z]+ [A-Z])\\.( [A-Z][a-z]+)".r.replaceAllIn(s, "$1&#46;$2")
    val res = regExp.replaceAllIn(namesParsed, m => {
      idx = idx + 1;
      "<span selectable=\"\" id=\"s" + idx.toString + "\">" + m.group("match") + "</span>"
    })
    res
  }

  def tagWords(s: String): String = {
    val regExp = new Regex("(?<![</&])\\b([a-zA-Z_']+)\\b", "match")
    var idx = 0
    regExp.replaceAllIn(s, m => {
      idx = idx + 1;
      "<span selectable=\"\" id=\"s" + idx.toString + "\">" + m.group("match") + "</span>"
    })
  }

  private object RemoveCorrectTags extends RewriteRule {
    override def transform(n: Node): Seq[Node] = {
      n match {
        case e: Elem if (e.label == "correct") => e.child.head
        case _ => n
      }
    }
  }

  def getHeadHtml(toPrint: Boolean): String = {
    val jspath = if (toPrint) QtiScriptLoader.JS_PRINT_PATH else QtiScriptLoader.JS_PATH
    val csspath = if (toPrint) QtiScriptLoader.CSS_PRINT_PATH else QtiScriptLoader.CSS_PATH

    def jsAndCss(name: String) = Seq(script(jspath + name + ".js"), css(csspath + name + ".css")).mkString("\n")
    jsAndCss("selectTextInteraction") + "\n"
  }

  private def css(url: String): String = """<link rel="stylesheet" type="text/css" href="%s"/>""".format(url)

  private def script(url: String): String = """<script type="text/javascript" src="%s"></script>""".format(url)

}
