package org.corespring.qti.models.interactions

import java.util.regex.Pattern
import org.corespring.qti.models.responses.{ ArrayResponse, StringResponse, ResponseOutcome, Response }
import org.corespring.qti.models.QtiItem.Correctness
import org.corespring.qti.models.{CorrectResponseEquation, CorrectResponseSingle, ResponseDeclaration}
import xml.Node
import org.corespring.qti.models.responses.javascript.DefaultResponseConverter

case class LineInteraction(responseIdentifier: String, override val locked: Boolean, override val nonInteractive:Boolean, sigfigs: Int = -1) extends Interaction with DefaultResponseConverter {
  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: Response): Option[ResponseOutcome] = {
    if (locked || nonInteractive) None else getOutcomeUnlocked(responseDeclaration, response)
  }
  private def getOutcomeUnlocked(responseDeclaration: Option[ResponseDeclaration], response: Response): Option[ResponseOutcome] = {
    def getSlopeAndYIntercept(equation: String): (Double, Double) = {
      val p = Pattern.compile("y=(.+)x\\+(.+)")
      val m = p.matcher(equation); m.matches()
      val slope = m.group(1).toDouble
      val yintercept = m.group(2).toDouble
      return (slope, yintercept)
    }
    def outcomeProperties(slope: Double, yintercept: Double): Map[String, Boolean] = {
      def toMap(value: String): Map[String, Boolean] = {
        val (slopeActual, yinterceptActual) = getSlopeAndYIntercept(value)
        if (slope == slopeActual && yintercept == yinterceptActual) Map("correct" -> true)
        else Map("incorrect" -> true)
      }
      responseDeclaration.map[Map[String, Boolean]](rd => rd.correctResponse match {
        case Some(CorrectResponseSingle(value)) => toMap(value)
        case Some(CorrectResponseEquation(value, _, _, _,_)) => toMap(value)
        case _ => Map()
      }).getOrElse(Map())
    }
    response match {
      case StringResponse(_, responseValue, _) => responseDeclaration match {
        case Some(rd) => rd.mapping match {
          case Some(mapping) => Some(
            ResponseOutcome(mapping.mappedValue(response.value),
              rd.isCorrect(responseValue) == Correctness.Correct,
              outcomeProperties = (outcomeProperties _).tupled(getSlopeAndYIntercept(responseValue))))
          case None => if (rd.isCorrect(response.value) == Correctness.Correct) {
            Some(ResponseOutcome(1, true, outcomeProperties = (outcomeProperties _).tupled(getSlopeAndYIntercept(responseValue))))
          } else Some(ResponseOutcome(0, false, outcomeProperties = (outcomeProperties _).tupled(getSlopeAndYIntercept(responseValue))))
        }
        case None => None
      }
      case ArrayResponse(_, responseValue, _) => responseDeclaration match {
        case Some(rd) => {
          val pta = responseValue(0).split(",").map(_.toDouble)
          val ptb = responseValue(1).split(",").map(_.toDouble)
          val slope = (pta(1) - ptb(1)) / (pta(0) - ptb(0))
          val yintercept = pta(1) - (slope * pta(0))
          val equation: String = if (sigfigs < 0) {
            "y=" + slope + "x+" + yintercept
          } else {
            val multiplier: Double = scala.math.pow(10, sigfigs)
            val roundedSlope = scala.math.floor(slope * multiplier) / multiplier
            val roundedYintercept = scala.math.floor(yintercept * multiplier) / multiplier
            "y=" + roundedSlope + "x+" + roundedYintercept
          }
          rd.mapping match {
            case Some(mapping) => Some(
              ResponseOutcome(mapping.mappedValue(equation),
                rd.isCorrect(equation) == Correctness.Correct,
                outcomeProperties = outcomeProperties(slope, yintercept)))
            case None => if (rd.isCorrect(equation) == Correctness.Correct) {
              Some(ResponseOutcome(1, true, outcomeProperties = outcomeProperties(slope, yintercept)))
            } else Some(ResponseOutcome(0, false, outcomeProperties = outcomeProperties(slope, yintercept)))
          }
        }
        case None => None
      }
      case _ => {
        logger.error("received a response that was not a string response in LineInteraction.getOutcome")
        None
      }
    }
  }

  def isScoreable: Boolean = true
}
object LineInteraction extends InteractionCompanion[LineInteraction] {
  def tagName: String = "lineInteraction"

  def apply(interaction: Node, itemBody: Option[Node]): LineInteraction = {
    LineInteraction(
      (interaction \ "@responseIdentifier").text,
      interaction.attribute("locked").isDefined,
      interaction.attribute("noninteractive").isDefined,
      (interaction \ "@sigfigs").text match {
        case "" => -1
        case sigfigs if sigfigs != "" && sigfigs.forall(_.isDigit) => sigfigs.toInt
        case _ => -1
      })

  }

  def parse(itemBody: Node): Seq[Interaction] = {
    val interactions = (itemBody \\ tagName)
    if (interactions.isEmpty) {
      Seq()
    } else {
      interactions.map(node => LineInteraction(node, Some(itemBody)))
    }
  }
}
