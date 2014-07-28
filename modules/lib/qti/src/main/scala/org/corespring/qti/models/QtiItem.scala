package org.corespring.qti.models

import org.corespring.qti.models.QtiItem.Correctness
import org.corespring.qti.models.interactions._
import scala.Some
import scala.xml._
import org.corespring.qti.models.responses.processing.ResponseProcessing

case class QtiItem(responseDeclarations: Seq[ResponseDeclaration], itemBody: ItemBody, modalFeedbacks: Seq[FeedbackInline], responseProcessing: Option[ResponseProcessing] = None){
  var defaultCorrect = "Correct!"
  var defaultIncorrect = "Your answer"

  val interactionsWithNoResponseDeclaration: Seq[Interaction] =
    itemBody.interactions.filterNot {
      interaction =>
        responseDeclarations.exists {
          rd =>
            rd.identifier == interaction.responseIdentifier
        }
    }

  lazy val isQtiValid: (Boolean, Seq[String]) = {
    val messages = itemBody.interactions.collect {
      case s => s.validate(this)._2
    }
    (itemBody.interactions.foldLeft(true)(_ && _.validate(this)._1), messages)
  }

  require(isQtiValid._1,
    "Invalid QTI: " + isQtiValid._2.mkString(", "))

  /**
   * Does the given interaction need correct responses?
   * @param id - the interaction id
   */
  def isCorrectResponseApplicable(id: String): Boolean = itemBody.getInteraction(id) match {
    case Some(TextEntryInteraction(_, _, _)) => false
    case Some(InlineChoiceInteraction(_, _)) => false
    case Some(LineInteraction(_, _, _, _)) => false
    case _ => true
  }

  /**
   * Check whether the entire response is correct
   * @param responseIdentifier
   * @param value
   * @return
   */
  def isCorrect(responseIdentifier: String, value: String): Correctness.Value = {
    responseDeclarations.find(_.identifier == responseIdentifier) match {
      case Some(rd) => rd.isCorrect(value)
      case _ => Correctness.Unknown
    }
  }

  /**
   * Checks whether the individual value is one of the values in the correct response.
   * So for a question id of "Q" : "Name the first 3 letters of the alphabet" ->
   * {{{
   * isValueCorrect("Q", "A", 0) -> true
   * isValueCorrect("Q", "B", 0) -> true
   * isValueCorrect("Q", "D", 0) -> false
   * }}}
   *
   * @param index - useful when checking the value against a [[org.corespring.qti.models.CorrectResponseOrdered]] - but may not be required
   * @return
   */
  def isValueCorrect(responseIdentifier: String, value: String, index: Int = 0): Boolean = {
    responseDeclarations.find(_.identifier == responseIdentifier) match {
      case Some(rd) => rd.isValueCorrect(value, Some(index))
      case _ => false
    }
  }

  /**
   * Get FeedbackInline with given id and value.
   * First looks for it in the feedbackBlocks, then in interaction feedbacks
   * finally if looks for a feedback that is flagged as incorrectResponse
   * @param id - the question responseIdentifier
   * @param value - the choice identifier
   * @return some FeedbackInline or None
   */
  def getFeedback(id: String, value: String): Option[FeedbackInline] = {
    responseDeclarations.find(rd => rd.identifier == id) match {
      case Some(responseDeclaration) => {
        getFeedbackBlock(id, responseDeclaration, value) orElse
          getFeedbackInline(id, value) orElse
          getFeedbackWithIncorrectResponse(id) orElse None
      }
      case _ => None
    }
  }

  private def getFeedbackWithIncorrectResponse(id: String): Option[FeedbackInline] = {
    itemBody.interactions.find(_.responseIdentifier == id) match {
      case Some(TextEntryInteraction(_, _, blocks)) => {
        val fb = blocks.find(_.incorrectResponse)
        fb
      }
      case _ => None
    }
  }

  private def getFeedbackBlock(id: String, rd: ResponseDeclaration, value: String): Option[FeedbackInline] = {
    itemBody.feedbackBlocks
      .filter(_.outcomeIdentifier == id)
      .find(b => rd.processInput(b.identifier) == rd.processInput(value)) match {
      case Some(fb) => {
        Some(fb)
      }
      case None => None
    }
  }

  private def getFeedbackInline(id: String, value: String): Option[FeedbackInline] = {

    itemBody.interactions.find(i => i.isInstanceOf[InteractionWithChoices] && i.responseIdentifier == id) match {
      case Some(i) => {
        i.asInstanceOf[InteractionWithChoices].getChoice(value) match {
          case Some(choice) => {
            val fb = choice.getFeedback
            fb
          }
          case None => None
        }
      }
      case _ => None
    }
  }
}

object QtiItem {
  val interactionModels: Seq[InteractionCompanion[_ <: Interaction]] = Seq(
    TextEntryInteraction,
    InlineChoiceInteraction,
    ChoiceInteraction,
    OrderInteraction,
    ExtendedTextInteraction,
    SelectTextInteraction,
    FocusTaskInteraction,
    LineInteraction,
    DragAndDropInteraction,
    PointInteraction)

  /**
   * An enumeration of the possible Correctness of a question
   */
  object Correctness extends Enumeration {
    type Correctness = Value
    val Correct, Incorrect, Unknown = Value
  }

  /**
   * Builds a [[org.corespring.qti.models.QtiItem]] from the qti xml
   * @param node - qti formatted xml, with some additional attributes added like csFeedbackId
   * @return
   */
  def apply(node: Node): QtiItem = {
    val qtiItem = createItem(node)
    addCorrectResponseFeedback(qtiItem, node)
    addIncorrectResponseFeedback(qtiItem, node)
    qtiItem
  }

  private def createItem(n: Node): QtiItem = {
    val itemBody = ItemBody((n \ "itemBody").head)
    QtiItem(
      responseDeclarations = (n \ "responseDeclaration").map(ResponseDeclaration(_, itemBody)),
      itemBody = itemBody,
      modalFeedbacks = (n \ "modalFeedbacks").map(FeedbackInline(_, None)),
      responseProcessing = (n \ "responseProcessing").headOption match {
        case Some(node) => Some(ResponseProcessing(itemBody, node))
        case _ => None
      })
  }

  private def addCorrectResponseFeedback(qti: QtiItem, n: Node) {
    (n \ "correctResponseFeedback").headOption match {
      case Some(correctResponseFeedback) => qti.defaultCorrect =
        clearNamespaceAndTransform(correctResponseFeedback,Seq(addTargetToAnchor _)).child.flatten mkString
      case None =>
    }
  }

  private def addTargetToAnchor(node:Node): Node = node match {
    case node:Elem => node match {
        case <a>{ n @ _* }</a> => node % new UnprefixedAttribute("target","_blank",Null)
        case _ => node
    }
    case _ => node
  }

  private def clearNamespaceAndTransform(node: Node, transformations: Seq[Node => Node]): Node = {

    val composedFuncs = transformations reduce (_ andThen _)

    def traverse (node: Node): Node =  composedFuncs(node) match {
      case elem:Elem => elem.copy(scope = TopScope, child = node.child.map(traverse))
      case _ => node
    }

    traverse(node)
  }

  private def addIncorrectResponseFeedback(qti: QtiItem, n: Node) {
    (n \ "incorrectResponseFeedback").headOption match {
      case Some(incorrectResponseFeedback) => qti.defaultIncorrect =
        clearNamespaceAndTransform(incorrectResponseFeedback,Seq(addTargetToAnchor _)).child.flatten mkString
      case None =>
    }
  }
}

case class ResponseDeclaration(identifier: String, cardinality: String, baseType: String, exactMatch: Boolean, correctResponse: Option[CorrectResponse], mapping: Option[Mapping]) {

  val  _correctResponse = exactMatch match {
    case false => correctResponse match {
      case Some(cr) => Some(cr match {
        case CorrectResponseAny(value) => CorrectResponseAny(value.map(processInput(_)))
        case CorrectResponseEquation(value, range, variables, numOfTestPoints,sigfigs) => CorrectResponseEquation(processInput(value), range, variables, numOfTestPoints,sigfigs)
        case CorrectResponseMultiple(value) => CorrectResponseMultiple(value.map(processInput(_)))
        case CorrectResponseOrdered(value) => CorrectResponseOrdered(value.map(processInput(_)))
        case CorrectResponseSingle(value) => CorrectResponseSingle(processInput(value))
        case CorrectResponseTargeted(_, _) => cr
        case _ => throw new Exception("Could not match")
      })
      case _ => None
    }
    case _ => correctResponse
  }

  def isCorrect(responseValues: Seq[String]): Correctness.Value = {
    isCorrect(responseValues.foldRight[String]("")((response, acc) => if (acc.isEmpty) processInput(response) else acc + "," + processInput(response)))
  }

  def isCorrect(responseValue: String): Correctness.Value = _correctResponse match {
    case Some(cr) => if (cr.isCorrect(processInput(responseValue))) Correctness.Correct else Correctness.Incorrect
    case None => Correctness.Unknown
  }

  def isPartOfCorrect(responseValue: String): Correctness.Value = _correctResponse match {
    case Some(cr) => if (cr.isPartOfCorrect(processInput(responseValue))) Correctness.Correct else Correctness.Incorrect
    case None => Correctness.Unknown
  }

  def isValueCorrect(value: String, index: Option[Int]): Boolean = _correctResponse match {
    case Some(cr) => cr.isValueCorrect(processInput(value), index)
    case None => false
  }

  def mappedValue(mapKey: String): Float = mapping match {
    case Some(m) => m.mappedValue(mapKey)
    case None => throw new RuntimeException("no mapping for this response declaration")
  }

  /**
   * This method does any preprocessing for a response before it is looked at for correctness
   */
  def processInput(response: String) = exactMatch match {
    case true => response
    case false => response.replaceAllLiterally(" ", "").toLowerCase
  }

  def hasDefaultCorrectResponse = correctResponse match {
    case None => false
    case _ => true
  }

}

object ResponseDeclaration {
  def apply(node: Node, body: ItemBody): ResponseDeclaration = {
    val identifier = (node \ "@identifier").text
    val cardinality = (node \ "@cardinality").text
    val baseType = (node \ "@baseType").text
    val correctResponseNode = (node \ "correctResponse").headOption
    val exactMatch = (node \ "@exactMatch").isEmpty match {
      case false => (node \ "@exactMatch").text != "false"
      case _ => true
    }

    require(!identifier.isEmpty, "identifier is empty for node: \n" + node)
    require(!cardinality.isEmpty, "cardinality is empty for node: \n" + node)

    ResponseDeclaration(
      identifier = identifier,
      cardinality = cardinality,
      baseType = baseType,
      correctResponse = buildCorrectResponse(correctResponseNode, identifier, cardinality, baseType, body),
      exactMatch = exactMatch,
      mapping = (node \ "mapping").headOption.map(Mapping(_)))
  }

  private def buildCorrectResponse(n: Option[Node], identifier: String, cardinality: String, baseType: String, body: ItemBody): Option[CorrectResponse] = n match {
    case None => None
    case Some(node) => {
      val maybeInteraction = body.getInteraction(identifier)
      Some(CorrectResponse(node, cardinality, baseType, maybeInteraction))
    }
  }
}

case class Mapping(mapEntries: Map[String, Float], defaultValue: Option[Float]) {
  def mappedValue(mapKey: String): Float = {
    mapEntries.get(mapKey) match {
      case Some(mappedValue) => mappedValue
      case None => defaultValue match {
        case Some(dv) => dv
        case None => throw new RuntimeException("no value found for given key")
      }
    }
  }
}

object Mapping {
  def apply(node: Node): Mapping = {
    val defaultValue = (node \ "@defaultValue").text match {
      case x: String if x.nonEmpty => Some(x.toFloat)
      case _ => None
    }
    val mapEntries = (node \ "mapEntry").foldRight[Map[String, Float]](Map())((node, acc) =>
      acc + ((node \ "@mapKey").text -> (node \ "@mappedValue").text.toFloat))
    Mapping(mapEntries, defaultValue)
  }
}

case class ItemBody(interactions: Seq[Interaction], feedbackBlocks: Seq[FeedbackInline]) {

  def getInteraction(id: String): Option[Interaction] = interactions.find(_.responseIdentifier == id)
}

object ItemBody {
  def apply(node: Node): ItemBody = {
    val feedbackBlocks = buildTypes[FeedbackInline](node, Seq(
      ("feedbackBlock", FeedbackInline(_, None))))
    ItemBody(QtiItem.interactionModels.map(_.parse(node)).flatten, feedbackBlocks)
  }

  private def buildTypes[T](node: Node, names: Seq[(String, (Node) => T)]): List[T] = {
    if (names.isEmpty) List()
    else {
      val name = names.head._1
      val fn = names.head._2
      val nodes: Seq[Node] = (node \\ name)
      val interactions: Seq[T] = nodes.map((n: Node) => fn(n))
      interactions.toList ::: buildTypes[T](node, names.tail)
    }
  }
}

