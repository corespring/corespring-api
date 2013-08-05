package org.corespring.qti.models.interactions

import controllers.Utils.isTrue
import org.corespring.qti.models.responses._
import org.corespring.qti.models.QtiItem.Correctness
import org.corespring.qti.models.interactions.choices.SimpleChoice
import org.corespring.qti.models.{CorrectResponseTargeted, ResponseDeclaration, QtiItem}
import scala.Some
import scala.collection.mutable
import scala.xml._
import scala.xml.transform.{RuleTransformer, RewriteRule}
import scala.xml.{Text, Attribute}

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

              case _ => (false, "Correct response declaration not found")

            }
          case _ => (false, "Drag and drop interaction not found")
        }
      case _ => (false, "Response declaration not found")
    }
  }

  def getChoice(identifier: String) = choices.find(_.identifier == identifier)

  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: Response): Option[ResponseOutcome] = {
    val (score:Int, isCorrect:Boolean) =
    response match {
      case ArrayResponse(_, responseValue, _) => responseDeclaration match {
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
            (sum, rd.isCorrect(responseValue) == Correctness.Correct)
          }
          case None => if (rd.isCorrect(response.value) == Correctness.Correct) {
            (1, true)
          } else {
            (0, false)
          }
        }
        case None => None
      }

      case _ => {
        Logger.error("received a response that was not a string response in ChoiceInteraction.getOutcome")
        None
      }
    }
    Some(ResponseOutcome(score, isCorrect, None, Map("responseCorrect"->isCorrect, "responseIncorrect"->(!isCorrect))))
  }
}

object DragAndDropInteraction extends InteractionCompanion[DragAndDropInteraction] {
  val answerNodeLabel = "draggableChoice"
  val targetNodeLabel = "landingPlace"
  val groupNodeLabel = "draggableChoiceGroup"
  val landingGroupNodeLabel = "landingPlaceGroup"
  val itemsPerRowAttribute = "itemsPerRow"
  val shuffleAttribute = "shuffle"
  val fixedAttribute = "fixed"

  def tagName = "dragAndDropInteraction"

  private def convertGroupToTable(xml: NodeSeq, nodeLabel:String, groupLabel:String, tableClass:String) = {

    val tdNode = <td></td>
    val trNode = <tr></tr>

    def addTdTrs(parent: Elem, shuffle: Boolean, answersPerRow: Int) = {
      require(answersPerRow > 0)

      def doShuffle(xs: Seq[Node]) = {
        def isFixed(n: Node): Boolean = isTrue(n.attribute(fixedAttribute))
        controllers.Utils.shuffle(xs, isFixed)
      }

      def tdRule = new RewriteRule {
        override def transform(n: Node): NodeSeq = {
          n match {
            case e: Elem if (e.label == nodeLabel) => tdNode.copy(child = e)
            case n => n
          }
        }
      }

      def trRule = new RewriteRule {
        override def transform(n: Node): NodeSeq = {
          n match {
            case el: Elem if (el.label == groupLabel) =>
              val trList = mutable.MutableList[Node]()
              val draggableAnswers = mutable.Queue[Node]() ++= (if (shuffle) doShuffle(n \\ nodeLabel) else n \\ nodeLabel)
              var currentTrNodes = mutable.MutableList[Node]()
              n.child.foreach {
                child =>

                  currentTrNodes += (if (child.label == nodeLabel) draggableAnswers.dequeue else child)
                  if ((currentTrNodes \\ nodeLabel).size == answersPerRow) {
                    trList += trNode.copy(child = currentTrNodes)
                    currentTrNodes = mutable.MutableList[Node]()
                  }
              }
              if (!currentTrNodes.isEmpty) trList += trNode.copy(child = currentTrNodes)
              el.copy(label = "table", child = trList.toSeq) % Attribute(None, "class", Text(tableClass), Null)

            case _ => n
          }
        }
      }

      new RuleTransformer(tdRule).transform(new RuleTransformer(trRule).transform(parent))
    }

    def mainRule = new RewriteRule {
      override def transform(n: Node): NodeSeq = {
        n match {
          case e: Elem if (e.label == groupLabel) =>
            e.attribute(itemsPerRowAttribute) match {
              case Some(v) => addTdTrs(e, isTrue(e.attribute(shuffleAttribute)), v.text.toInt)
              case _ => n
            }

          case n => n
        }
      }
    }

    new RuleTransformer(mainRule).transform(xml)
  }

  override def preProcessXml(interactionXml: Elem): NodeSeq = {
    convertGroupToTable(convertGroupToTable(interactionXml, answerNodeLabel, groupNodeLabel, "csDraggableTiles"), targetNodeLabel, landingGroupNodeLabel, "csDropTiles")
  }

  def apply(node: Node, itemBody: Option[Node]): DragAndDropInteraction = DragAndDropInteraction(
    (node \ "@responseIdentifier").text,
    (node \\ answerNodeLabel).map(SimpleChoice(_, (node \ "@responseIdentifier").text)),
    (node \\ targetNodeLabel).map(n => Target((n \ "@identifier").text, (n \ "@cardinality").text)).toSeq
  )

  def parse(itemBody: Node): Seq[Interaction] = {
    val interactions = (itemBody \\ tagName)
    if (interactions.isEmpty) {
      Seq()
    } else {
      interactions.map(node => DragAndDropInteraction(node, Some(itemBody)))
    }
  }
}
