package qti.models.interactions

import xml.{XML, NodeSeq, Elem, Node}
import xml.transform.{RuleTransformer, RewriteRule}
import util.matching.Regex
import qti.models.ResponseDeclaration
import models.{ItemResponseOutcome, ItemResponse}
import testplayer.views.utils.QtiScriptLoader

case class SelectTextInteraction(responseIdentifier: String, selectionType: String, minSelection: Int, maxSelection: Int) extends Interaction {
  def getChoice(identifier: String) = None
  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: ItemResponse) : Option[ItemResponseOutcome] = None
}

object SelectTextInteraction extends InteractionCompanion[SelectTextInteraction]{
  def apply(node: Node,itemBody:Option[Node]): SelectTextInteraction = SelectTextInteraction(
    (node \ "@responseIdentifier").text,
    (node \ "@selectionType").text,
    (node \ "@minSelections").text.toInt,
    (node \ "@maxSelections").text.toInt
  )
  def parse(itemBody:Node):Seq[Interaction] = {
    val interactions = (itemBody \\ "selectTextInteraction")
    if (interactions.isEmpty){
      Seq()
    }else{
      interactions.map(node => SelectTextInteraction(node,Some(itemBody)))
    }
  }
  def interactionMatch(e:Elem):Boolean = e.label == "selectTextInteraction"
  override def preProcessXml(interactionXml:Elem):NodeSeq = tokenizeSelectText(interactionXml)


  private def tokenizeSelectText(interactionXml:NodeSeq):NodeSeq = {
    val isWord = (interactionXml \ "@selectionType").text == "word"
    val taggedXml = if (isWord) performOnText(interactionXml, tagWords) else performOnText(interactionXml, tagSentences)
    new RuleTransformer(RemoveCorrectTags).transform(taggedXml)
  }

  private def performOnText(e:NodeSeq, fn: String=>String):NodeSeq = {
    val xmlText = e.mkString
    val regExp = "<.*?>".r
    val matches = regExp.findAllIn(xmlText).toList

    val openString = matches.head
    val closeString = regExp.findAllIn(xmlText).toList.last

    val lastIndex = xmlText.lastIndexOf("<")
    val resultText = xmlText.substring(0, lastIndex).replaceFirst("<.*?>","")

    val transformedText = fn(resultText)

    XML.loadString(openString + transformedText + closeString)
  }
  private def tagWords(s:String):String = {
    val regExp = new Regex("(?<![</&])\\b([a-zA-Z_']+)\\b", "match")
    var idx = 0
    regExp.replaceAllIn(s, m => { idx = idx + 1; "<span selectable=\"\" id=\"s"+idx.toString+"\">"+m.group("match")+"</span>"})
  }
  private def tagSentences(s:String):String = {
    val regExp = new Regex("(?s)(.*?[.!?](<\\/correct>)*)", "match")
    var idx = 0
    val res = regExp.replaceAllIn(s, m => { idx = idx + 1; "<span selectable=\"\" id=\"s"+idx.toString+"\">"+m.group("match")+"</span>"})
    res
  }

  private object RemoveCorrectTags extends RewriteRule {
    override def transform(n: Node): Seq[Node] = {
      n match {
        case e:Elem if(e.label == "correct") => e.child.head
        case _ => n
      }
    }
  }

  def getHeadHtml(toPrint:Boolean):String = {
    val jspath = if (toPrint) QtiScriptLoader.JS_PRINT_PATH else QtiScriptLoader.JS_PATH
    val csspath = if (toPrint) QtiScriptLoader.CSS_PRINT_PATH else QtiScriptLoader.CSS_PATH

    def jsAndCss(name:String) = Seq(script(jspath + name + ".js"), css(csspath + name + ".css")).mkString("\n")
    jsAndCss("selectTextInteraction")+"\n"
  }
  private def css(url: String): String = """<link rel="stylesheet" type="text/css" href="%s"/>""".format(url)
  private def script(url: String): String = """<script type="text/javascript" src="%s"></script>""".format(url)
}
