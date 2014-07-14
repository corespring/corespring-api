package org.corespring.qti.models.interactions

import org.corespring.qti.models.responses.{ ResponseOutcome, ArrayResponse, Response }
import org.corespring.qti.models._
import scala.Some
import scala.language.postfixOps
import util.matching.Regex
import xml.transform.{ RuleTransformer, RewriteRule }
import xml.{ XML, NodeSeq, Elem, Node }
import org.corespring.qti.models.responses.javascript.DefaultResponseConverter

case class SelectTextInteraction(responseIdentifier: String, selectionType: String, checkIfCorrect: Boolean, minSelection: Int, maxSelection: Int, correctResponse: Option[CorrectResponseMultiple]) extends Interaction with DefaultResponseConverter {

  def isScoreable = true

  override def validate(qtiItem: QtiItem) = {
    (true, "Ok")
  }

  def getChoice(identifier: String) = None

  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: Response): Option[ResponseOutcome] = {
    var score: Float = 0
    var outcomeProperties: Map[String, Boolean] = Map()
    response match {
      case ArrayResponse(_, responseValue, _) => correctResponse match {
        case Some(cr) => {
          val isNumberOfSelectionCorrect = responseValue.size >= minSelection && responseValue.size <= maxSelection

          val isCorrect = (
            if (checkIfCorrect) {
              // We need to check for correctness as well
              val isEverySelectedCorrect = cr.isPartOfCorrect(response.value)
              if (!isEverySelectedCorrect)
                outcomeProperties = outcomeProperties + ("responsesIncorrect" -> true)

              isEverySelectedCorrect && isNumberOfSelectionCorrect
            } else {
              // We only need whether the number of selection is within range of [minSelection,maxSelection]
              isNumberOfSelectionCorrect
            })

          if (isNumberOfSelectionCorrect)
            outcomeProperties = outcomeProperties + ("responsesNumberCorrect" -> true)

          if (isCorrect) {
            score = 1
            outcomeProperties = outcomeProperties + ("responsesCorrect" -> true)
          }
          if (responseValue.size > maxSelection) {
            outcomeProperties = outcomeProperties + ("responsesExceedMax" -> true)
          } else {
            outcomeProperties = outcomeProperties + ("responsesExceedMax" -> false)
          }
          if (responseValue.size < minSelection) {
            outcomeProperties = outcomeProperties + ("responsesBelowMin" -> true)
          } else {
            outcomeProperties = outcomeProperties + ("responsesBelowMin" -> false)
          }
          Some(ResponseOutcome(score, score == 1, None, outcomeProperties))
        }
        case _ => None
      }
      case _ => {
        logger.error("received a response that was not an array response in SelectTextInteraction.getOutcome")
        None
      }
    }
  }

}

object SelectTextInteraction extends InteractionCompanion[SelectTextInteraction] {

  def tagName = "selectTextInteraction"

  def apply(node: Node, itemBody: Option[Node]): SelectTextInteraction = {

    val correctAnswers = Some(CorrectResponseMultiple(SelectTextInteraction.parseCorrectResponses(node)))

    SelectTextInteraction(
      (node \ "@responseIdentifier").text,
      (node \ "@selectionType").text,
      (node \ "@checkIfCorrect").text.toLowerCase == "yes",
      (node \ "@minSelections").text.toInt,
      (node \ "@maxSelections").text.toInt,
      correctAnswers)
  }

  def parse(itemBody: Node): Seq[Interaction] = {
    val interactions = (itemBody \\ "selectTextInteraction")
    if (interactions.isEmpty) {
      Seq()
    } else {
      interactions.map(node => SelectTextInteraction(node, Some(itemBody)))
    }
  }

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
          filterNot (_ \ "correct" isEmpty)).map(n => idRegexp.findFirstMatchIn(n.mkString).get.group("match"))
      }
    correctIndexes
  }

  def tokenizeSelectText(interactionXml: NodeSeq): NodeSeq = {
    val isWord = (interactionXml \ "@selectionType").text == "word"
    val taggedXml = if (isWord) performOnText(interactionXml, tagWords) else performOnText(interactionXml, tagSentences)
    new RuleTransformer(RemoveCorrectTags).transform(taggedXml)
  }

  private def performOnText(e: NodeSeq, fn: String => String): NodeSeq = {

    val xmlText = e.mkString
    val regExp = "<.*?>".r
    val matches = regExp.findAllIn(xmlText).toList

    def noBody = matches.length == 1

    if (noBody) {
      XML.loadString(matches.head)
    } else {
      val openString = matches.head
      val closeString = regExp.findAllIn(xmlText).toList.last

      val lastIndex = xmlText.lastIndexOf("<")
      val resultText = xmlText.substring(0, lastIndex).replaceFirst("<.*?>", "")
      val transformedText = fn(resultText)
      XML.loadString(openString + transformedText + closeString)
    }
  }

  private def tagSentences(s: String): String = {
    val regExp = new Regex("(?s)(.*?[.!?]([^ \\t])*)", "match")
    var idx = 0

    // Filter out names like Vikram S. Pandit as they break the sentence parsing
    val namesParsed = "([A-Z][a-z]+ [A-Z])\\.( [A-Z][a-z]+)".r.replaceAllIn(s, "$1&#46;$2")
    val res = regExp.replaceAllIn(namesParsed, m => {
      idx = idx + 1
      "<span selectable=\"\" id=\"s" + idx.toString + "\">" + m.group("match") + "</span>"
    })
    res
  }

  private def tagWords(s: String): String = {

    val regExp = new Regex("(?<![</&])\\b([a-zA-Z_']+)\\b", "match")
    var idx = 0
    regExp.replaceAllIn(s, m => {
      idx = idx + 1
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

}
