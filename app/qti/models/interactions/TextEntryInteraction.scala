package qti.models.interactions

import xml.{NodeSeq, Elem, Node}
import qti.models.ResponseDeclaration
import models.{StringItemResponse, ItemResponseOutcome, ItemResponse}
import qti.models.QtiItem.Correctness
import controllers.Log
import testplayer.views.utils.QtiScriptLoader

case class TextEntryInteraction(representingNode:Node, responseIdentifier: String, expectedLength: Int, feedbackBlocks: Seq[FeedbackInline]) extends Interaction {
  def getResponseDeclaration: Option[ResponseDeclaration] = None
  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: ItemResponse) : Option[ItemResponseOutcome] = {
    response match {
      case StringItemResponse(_,responseValue,_) => responseDeclaration match {
        case Some(rd) => rd.mapping match {
          case Some(mapping) => Some(ItemResponseOutcome(mapping.mappedValue(response.value)))
          case None => if (rd.isCorrect(response.value) == Correctness.Correct) {
            Some(ItemResponseOutcome(1))
          } else Some(ItemResponseOutcome(0))
        }
        case None => None
      }
      case _ => {
        Log.e("received a response that was not a string response in TextEntryInteraction.getOutcome")
        None
      }
    }
  }
}

object TextEntryInteraction extends InteractionCompanion[TextEntryInteraction]{
  def apply(node: Node, itemBody:Option[Node]): TextEntryInteraction = {
    val responseIdentifier = Interaction.responseIdentifier(node)
    TextEntryInteraction(
      representingNode = node,
      responseIdentifier = responseIdentifier,
      expectedLength = expectedLength(node),
      feedbackBlocks = itemBody match {
        case Some(node) => feedbackBlocks(node).filter(_.outcomeIdentifier == responseIdentifier)
        case None => throw new RuntimeException("this interaction requires a reference to the outer qti model");
      }
    )
  }
  def parse(itemBody:Node):Seq[Interaction] = {
    val interactions = (itemBody \\ "textEntryInteraction")
    if (interactions.isEmpty){
      Seq()
    }else{
      interactions.map(node => TextEntryInteraction(node,Some(itemBody)))
    }
  }
  private def feedbackBlocks(itemBody:Node):Seq[FeedbackInline] = {
    (itemBody \ "feedbackBlock").map(node => FeedbackInline(node,None))
  }
  private def expectedLength(n: Node): Int = (n \ "@expectedLength").text.toInt

  def interactionMatch(e:Elem):Boolean = e.label == "textEntryInteraction"

  def getHeadHtml(toPrint:Boolean):String = {
    val jspath = if (toPrint) QtiScriptLoader.JS_PRINT_PATH else QtiScriptLoader.JS_PATH
    val csspath = if (toPrint) QtiScriptLoader.CSS_PRINT_PATH else QtiScriptLoader.CSS_PATH

    def jsAndCss(name:String) = Seq(script(jspath + name + ".js"), css(csspath + name + ".css")).mkString("\n")
    jsAndCss("textEntryInteraction")+"\n"
  }
  private def css(url: String): String = """<link rel="stylesheet" type="text/css" href="%s"/>""".format(url)
  private def script(url: String): String = """<script type="text/javascript" src="%s"></script>""".format(url)
}
