package qti.models.interactions

import choices.{Choice, SimpleChoice}
import xml._
import models.{StringItemResponse, ItemResponseOutcome, ItemResponse}
import scala.Some
import xml.transform.{RewriteRule, RuleTransformer}
import scala.Null
import scala.Some
import xml.Text
import scala.Some
import qti.processors.FeedbackProcessor
import qti.models.ResponseDeclaration
import qti.models.QtiItem.Correctness
import controllers.Log
import testplayer.views.utils.QtiScriptLoader

case class ChoiceInteraction(responseIdentifier: String, choices: Seq[SimpleChoice]) extends InteractionWithChoices{
  def getChoice(identifier: String):Option[Choice] = choices.find(_.identifier == identifier)
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
        Log.e("received a response that was not a string response in ChoiceInteraction.getOutcome")
        None
      }
    }
  }
}

object ChoiceInteraction extends InteractionCompanion[ChoiceInteraction]{
  def apply(interaction: Node, itemBody: Option[Node]): ChoiceInteraction = ChoiceInteraction(
    (interaction \ "@responseIdentifier").text,
    (interaction \ "simpleChoice").map(SimpleChoice(_, (interaction \ "@responseIdentifier").text))
  )
  def parse(itemBody:Node):Seq[Interaction] = {
    val interactions = (itemBody \\ "choiceInteraction")
    if (interactions.isEmpty){
      Seq()
    }else{
      interactions.map(node => ChoiceInteraction(node,Some(itemBody)))
    }
  }
  def interactionMatch(e:Elem) = e.label == "choiceInteraction"
  override def preProcessXml(interactionXml:Elem):NodeSeq = {
    new InteractionProcessing.FeedbackOutcomeIdentifierInserter(ChoiceInteraction(interactionXml,None)).transform(interactionXml)
  }
  def getHeadHtml(toPrint:Boolean):String = {
    val jspath = if (toPrint) QtiScriptLoader.JS_PRINT_PATH else QtiScriptLoader.JS_PATH
    val csspath = if (toPrint) QtiScriptLoader.CSS_PRINT_PATH else QtiScriptLoader.CSS_PATH

    def jsAndCss(name:String) = Seq(script(jspath + name + ".js"), css(csspath + name + ".css")).mkString("\n")
    jsAndCss("choiceInteraction")+"\n"+jsAndCss("simpleChoice");
  }
  private def css(url: String): String = """<link rel="stylesheet" type="text/css" href="%s"/>""".format(url)
  private def script(url: String): String = """<script type="text/javascript" src="%s"></script>""".format(url)
}
