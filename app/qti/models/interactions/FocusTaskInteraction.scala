package qti.models.interactions

import choices.{Choice, SimpleChoice}
import xml._
import models.{ArrayItemResponse, ItemResponseOutcome, ItemResponse}
import scala.Some
import qti.models.{QtiItem, CorrectResponseMultiple, ResponseDeclaration}
import controllers.Log
import testplayer.views.utils.QtiScriptLoader

case class FocusTaskInteraction(responseIdentifier: String, choices: Seq[SimpleChoice], checkIfCorrect: Boolean, minSelections: Int, maxSelections: Int) extends InteractionWithChoices {
  override def validate(qtiItem: QtiItem) = {
    val hasResponse = !(qtiItem.responseDeclarations.find(_.identifier == responseIdentifier).isEmpty)
    if (!checkIfCorrect || (checkIfCorrect && hasResponse))
      (true, "Ok")
    else
      (false, "Missing response declartaion for " + responseIdentifier)
  }

  def getChoice(identifier: String): Option[Choice] = choices.find(_.identifier == identifier)

  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: ItemResponse): Option[ItemResponseOutcome] = {
    var outcomeProperties: Map[String, Boolean] = Map()
    var score: Float = 0
    var isResponseCorrect: Boolean = true
    var isResponseIncorrect: Boolean = false
    response match {
      case ArrayItemResponse(_, responseValues, _) =>
        val isNumberOfSelectionCorrect = responseValues.size >= minSelections && responseValues.size <= maxSelections
        responseDeclaration match {
          case Some(rd) =>

            if (checkIfCorrect) {
              rd.correctResponse match {
                case Some(cr: CorrectResponseMultiple) =>
                  isResponseCorrect = cr.isPartOfCorrect(response.value)
                  isResponseIncorrect = !isResponseCorrect
              }
            } else {
              isResponseCorrect = isNumberOfSelectionCorrect
            }

            score = rd.mapping match {
              case Some(mapping) =>
                responseValues.foldRight[Float](0)((responseValue, sum) => sum + mapping.mappedValue(responseValue))
              case _ =>
                if (isResponseCorrect) 1 else 0
            }
          case None =>
            isResponseCorrect = isNumberOfSelectionCorrect
            score = if (isResponseCorrect) 1 else 0
        }

        outcomeProperties = outcomeProperties + ("responsesNumberCorrect" -> isNumberOfSelectionCorrect)
        outcomeProperties = outcomeProperties + ("responsesIncorrect" -> isResponseIncorrect)
        outcomeProperties = outcomeProperties + ("responsesCorrect" -> (isResponseCorrect && isNumberOfSelectionCorrect))
        outcomeProperties = outcomeProperties + ("responsesExceedMax" -> (responseValues.size > maxSelections))
        outcomeProperties = outcomeProperties + ("responsesBelowMin" -> (responseValues.size < minSelections))

      case _ => {
        Log.e("received a response that was not an array response in FocusTaskInteraction.getOutcome")
        false
      }
    }


    Some(ItemResponseOutcome(score, None, outcomeProperties))
  }
}

object FocusTaskInteraction extends InteractionCompanion[FocusTaskInteraction] {

  def apply(interaction: Node, itemBody: Option[Node]): FocusTaskInteraction = {

    FocusTaskInteraction(
      (interaction \ "@responseIdentifier").text,
      (interaction \ "focusChoice").map(SimpleChoice(_, (interaction \ "@responseIdentifier").text)),
      (interaction \ "@checkIfCorrect").text == "yes",
      (interaction \ "@minSelections").text.toInt,
      (interaction \ "@maxSelections").text.toInt
    )
  }

  def parse(itemBody: Node): Seq[Interaction] = {
    val interactions = (itemBody \\ "focusTaskInteraction")
    if (interactions.isEmpty) {
      Seq()
    } else {
      interactions.map(node => FocusTaskInteraction(node, Some(itemBody)))
    }
  }

  def interactionMatch(e: Elem) = e.label == "focusTaskInteraction"

  override def preProcessXml(interactionXml: Elem): NodeSeq = {
    new InteractionProcessing.FeedbackOutcomeIdentifierInserter(FocusTaskInteraction(interactionXml, None)).transform(interactionXml)
  }

  def getHeadHtml(toPrint: Boolean): String = {
    val jspath = if (toPrint) QtiScriptLoader.JS_PRINT_PATH else QtiScriptLoader.JS_PATH
    val csspath = if (toPrint) QtiScriptLoader.CSS_PRINT_PATH else QtiScriptLoader.CSS_PATH

    def jsAndCss(name: String) = Seq(script(jspath + name + ".js"), css(csspath + name + ".css")).mkString("\n")
    jsAndCss("focusTask") + "\n" + jsAndCss("simpleChoice")
  }

  private def css(url: String): String = """<link rel="stylesheet" type="text/css" href="%s"/>""".format(url)

  private def script(url: String): String = """<script type="text/javascript" src="%s"></script>""".format(url)
}
