package qti.models.interactions

import choices.SimpleChoice
import models.itemSession._
import qti.models.QtiItem.Correctness
import qti.models.{CorrectResponseTargeted, QtiItem, ResponseDeclaration}
import xml.Node
import scala.collection.mutable

case class Target(identifier: String, cardinality: String)

case class DragAndDropInteraction(responseIdentifier: String, choices: Seq[SimpleChoice], targets: Seq[Target]) extends InteractionWithChoices {

  def isScoreable = true

  override def validate(qtiItem: QtiItem) = {
    qtiItem.responseDeclarations.find(_.identifier == responseIdentifier) match {
      case Some(responseDeclaration) =>
        qtiItem.itemBody.interactions.find(_.responseIdentifier == responseIdentifier) match {
          case Some(interaction: DragAndDropInteraction) =>
            responseDeclaration.correctResponse match {
              case Some(correctResponseTargeted: CorrectResponseTargeted) =>
                val targets = interaction.targets
                val errors = new mutable.MutableList[String]()

                // Match all targets in the response declaration to dragTarget nodes in the interaction
                correctResponseTargeted.value.find(t => !targets.exists(_.identifier == t._1)).foreach(
                  errors += "Target " + _._1 + " not found as dragTarget\n"
                )

                // Match all answers in the response declaration to a draggableAnswer node in the interaction
                correctResponseTargeted.value.foreach(
                  _._2.foreach(answer =>
                    if (!interaction.choices.exists(_.identifier == answer))
                      errors += answer + " is declared in response declaration but not found as draggable answer\n"
                  )
                )

                // Check if all dragTargets have their corresponding value in the response declaration
                targets.find(t => !correctResponseTargeted.value.contains(t.identifier)).foreach(
                  errors += "dragTarget " + _ + " has no response declaration\n"
                )

                // Find duplicates
                if (targets.distinct != targets) errors += "Some targets are duplicated in dragTargets"

                (errors.isEmpty, errors.mkString("\n"))

              case None => (false, "Correct response declaration not found")

            }
          case None => (false, "Drag and drop interaction not found")
        }
      case None => (false, "Response declaration not found")
    }
  }

  def getChoice(identifier: String) = choices.find(_.identifier == identifier)

  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: ItemResponse): Option[ItemResponseOutcome] = {
    response match {
      case ArrayItemResponse(_, responseValue, _) => responseDeclaration match {
        case Some(rd) => rd.mapping match {
          case Some(mapping) => {
            var count: Int = 0;
            var sum: Float = 0;
            var correctCount: Int = 0;
            for (value <- responseValue) {
              if (rd.isValueCorrect(value, Some(count))) {
                sum += mapping.mappedValue(value)
                correctCount += 1;
              }
              count += 1;
            }
            Some(ItemResponseOutcome(sum, rd.isCorrect(responseValue) == Correctness.Correct))
          }
          case None => if (rd.isCorrect(response.value) == Correctness.Correct) {
            Some(ItemResponseOutcome(1, true))
          } else {
            Some(ItemResponseOutcome(0, false))
          }
        }
        case None => None
      }
      case _ => {
        Logger.error("received a response that was not a string response in ChoiceInteraction.getOutcome")
        None
      }
    }
  }
}

object DragAndDropInteraction extends InteractionCompanion[DragAndDropInteraction] {
  val draggableAnswer = "draggableAnswer"
  val dragTarget = "dragTarget"

  def tagName = "dragAndDropInteraction"

  def apply(node: Node, itemBody: Option[Node]): DragAndDropInteraction = DragAndDropInteraction(
    (node \ "@responseIdentifier").text,
    (node \\ draggableAnswer).map(SimpleChoice(_, (node \ "@responseIdentifier").text)),
    (node \\ dragTarget).map(n => Target((n \ "@identifier").text, (n \ "@cardinality").text)).toSeq
  )

  def parse(itemBody: Node): Seq[Interaction] = {
    val interactions = (itemBody \\ "dragAndDropInteraction")
    if (interactions.isEmpty) {
      Seq()
    } else {
      interactions.map(node => DragAndDropInteraction(node, Some(itemBody)))
    }
  }
}
