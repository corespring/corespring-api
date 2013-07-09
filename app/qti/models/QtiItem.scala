package qti.models

import interactions._
import qti.models.QtiItem.Correctness
import scala.Some
import scala.xml._
import util.Random
import javax.script.{ScriptEngine, ScriptEngineManager}
import java.util.regex.Pattern

case class QtiItem(responseDeclarations: Seq[ResponseDeclaration], itemBody: ItemBody, modalFeedbacks: Seq[FeedbackInline]) {
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


  val isQtiValid:(Boolean, Seq[String]) = {
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
   * @param index - useful when checking the value against a [[qti.models.CorrectResponseOrdered]] - but may not be required
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
   * @param choiceId - the choice identifier
   * @return some FeedbackInline or None
   */
  def getFeedback(id: String, choiceId: String): Option[FeedbackInline] = {
    val fb = pf(getFeedbackBlock(id, choiceId)) orElse
      pf(getFeedbackInline(id, choiceId)) orElse
      pf(getFeedbackWithIncorrectResponse(id)) orElse None

    fb
  }


  private def pf[T]: PartialFunction[Option[T], Option[T]] = {
    case Some(thing) => Some(thing)
    case None => None
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

  private def getFeedbackBlock(id: String, value: String): Option[FeedbackInline] = {
    itemBody.feedbackBlocks
      .filter(_.outcomeIdentifier == id)
      .find(_.identifier == value) match {
      case Some(fb) => {
        Some(fb)
      }
      case None => None
    }
  }

  private def getFeedbackInline(id: String, value: String): Option[FeedbackInline] = {

    itemBody.interactions.find(i => i.isInstanceOf[InteractionWithChoices] &&  i.responseIdentifier == id) match {
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
  val interactionModels:Seq[InteractionCompanion[_ <: Interaction]] = Seq(
    TextEntryInteraction,
    InlineChoiceInteraction,
    ChoiceInteraction,
    OrderInteraction,
    ExtendedTextInteraction,
    SelectTextInteraction,
    FocusTaskInteraction,
    LineInteraction
  )
  /**
   * An enumeration of the possible Correctness of a question
   */
  object Correctness extends Enumeration {
    type Correctness = Value
    val Correct, Incorrect, Unknown = Value
  }

  /**
   * Builds a [[qti.models.QtiItem]] from the qti xml
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
 //   val customResponseDeclarations = itemBody.interactions.map(i=>i.getResponseDeclaration.getOrElse(ResponseDeclaration("", "", None, None))).filterNot(_.identifier == "")
    QtiItem(
      responseDeclarations = (n \ "responseDeclaration").map(ResponseDeclaration(_, itemBody)),
      itemBody = itemBody,
      modalFeedbacks = (n \ "modalFeedbacks").map(FeedbackInline(_, None))
    )
  }

  private def addCorrectResponseFeedback(qti: QtiItem, n: Node) {
    (n \ "correctResponseFeedback").headOption match {
      case Some(correctResponseFeedback) => qti.defaultCorrect = correctResponseFeedback.child.text
      case None =>
    }
  }

  private def addIncorrectResponseFeedback(qti: QtiItem, n: Node) {
    (n \ "incorrectResponseFeedback").headOption match {
      case Some(incorrectResponseFeedback) => qti.defaultIncorrect = incorrectResponseFeedback.child.text
      case None =>
    }
  }

}

case class ResponseDeclaration(identifier: String, cardinality: String, correctResponse: Option[CorrectResponse], mapping: Option[Mapping]) {
  def isCorrect(responseValues: Seq[String]): Correctness.Value = {
    isCorrect(responseValues.foldRight[String]("")((response,acc) => if(acc.isEmpty) response else acc+","+response))
  }
  def isCorrect(responseValue: String): Correctness.Value = correctResponse match {
    case Some(cr) => if (cr.isCorrect(responseValue)) Correctness.Correct else Correctness.Incorrect
    case None => Correctness.Unknown
  }
  def isValueCorrect(value: String, index: Option[Int]): Boolean = correctResponse match {
    case Some(cr) => cr.isValueCorrect(value, index)
    case None => false
  }
  def mappedValue(mapKey: String): Float = mapping match {
    case Some(m) => m.mappedValue(mapKey)
    case None => throw new RuntimeException("no mapping for this response declaration")
  }

}

object ResponseDeclaration {
  def apply(node: Node, body: ItemBody): ResponseDeclaration = {
    val identifier = (node \ "@identifier").text
    val cardinality = (node \ "@cardinality").text
    val correctResponseNode = (node \ "correctResponse").headOption

    require(!identifier.isEmpty, "identifier is empty for node: \n" + node)
    require(!cardinality.isEmpty, "cardinality is empty for node: \n" + node)

    ResponseDeclaration(
      identifier = identifier,
      cardinality = cardinality,
      correctResponse = buildCorrectResponse(correctResponseNode, identifier, cardinality, body),
      mapping = (node \ "mapping").headOption.map(Mapping(_))
    )
  }

  private def buildCorrectResponse(n: Option[Node], identifier: String, cardinality: String, body: ItemBody): Option[CorrectResponse] = n match {
    case None => None
    case Some(node) => {
      val maybeInteraction = body.getInteraction(identifier)
      Some(CorrectResponse(node, cardinality, maybeInteraction))
    }
  }
}

trait CorrectResponse {
  def isCorrect(responseValue: String): Boolean

  def isValueCorrect(value: String, index: Option[Int]): Boolean
}

object CorrectResponse {

  /**
   * Note: TextEntryInteractions are a special case. If cardinality is equation, use CorrectResponseEquation, otherwise,
   * no matter what the cardinality is we treat their responses as CorrectResponseAny
   * @param node
   * @param cardinality
   * @param interaction
   * @return
   */
  def apply(node: Node, cardinality: String, interaction: Option[Interaction] = None): CorrectResponse = interaction match {
    case Some(TextEntryInteraction(_, _, _)) => cardinality match {
      case "equation" => CorrectResponseEquation(node)
      case _ => CorrectResponseAny(node)
    }
    case Some(SelectTextInteraction(_, _, _, _, _, _)) => CorrectResponseAny(node)
    case _ => cardinality match {
      case "single" => CorrectResponseSingle(node)
      case "multiple" => CorrectResponseMultiple(node)
      case "ordered" => CorrectResponseOrdered(node)
      case "equation" => CorrectResponseEquation(node)
      case _ => throw new RuntimeException("unknown cardinality: " + cardinality + ". cannot generate CorrectResponse")
    }
  }
}

/**
 * value is limited to the format y=f(x), where f(x) is some continuous (defined at all points) expression only containing the variable x
 */
case class CorrectResponseEquation(value: String,
                                   useInteger:Boolean = true,
                                   range:(Int,Int) = (-10,10),
                                   variables:(String,String) = "x" -> "y",
                                   numOfTestPoints:Int = 20) extends CorrectResponse{
  private val engine = CorrectResponseEquation.engine
  private def formatExpression(expr:String,variableValues:Seq[(String,Int)]):String = {
    val noWhitespace = expr.replaceAll("\\s","")
    variableValues.foldRight[String](noWhitespace)((variable,acc) =>{
      replaceVar(acc,variable._1,variable._2)
    })
  }
  private def replaceVar(expr:String, variable:String, num:Int):String = {
    var newExpr = expr
    var m = Pattern.compile(".*([0-9)])"+variable+"([(0-9]).*").matcher(newExpr)
    if (m.matches()){
      newExpr = newExpr.replaceAll("[0-9)]"+variable+"[(0-9]",m.group(1)+"*("+num.toString+")*"+m.group(2))
    }
    m = Pattern.compile(".*([0-9)])"+variable+".*").matcher(newExpr)
    if (m.matches()){
      newExpr = newExpr.replaceAll("[0-9)]"+variable,m.group(1)+"*("+num.toString+")")
    }
    m = Pattern.compile(".*"+variable+"([(0-9]).*").matcher(newExpr)
    if (m.matches()){
      newExpr = newExpr.replaceAll(variable+"[(0-9]","("+num.toString+")*"+m.group(2))
    }
    newExpr = newExpr.replaceAll(variable,"("+num.toString+")")
    newExpr
  }
  /**
   * find coordinates on the graph that fall on the line
   */
  lazy val testPoints:Array[(Int,Int)] = {
    val rhs = value.split("=")(1)
    var testCoords:Array[(Int,Int)] = Array()
    for (i <- 1 to numOfTestPoints){
      val xcoord = new Random().nextInt(range._2 - range._1)+range._1
      val ycoord = engine.eval(formatExpression(rhs,Seq(variables._1 -> xcoord))).asInstanceOf[Double].toInt
      testCoords = testCoords :+ (xcoord,ycoord)
    }
    testCoords
  }
  /**
   * compare response equation with value equation. Since there are many possible forms, we generate random points
   */
  private def compareEquations(response:String):Boolean = {
    val sides = response.split("=")
    if (sides.length == 2){
      val lhs = sides(0)
      val rhs = sides(1)
      testPoints.foldRight[Boolean](true)((testPoint,acc) => if (acc){
        val variableValues = Seq(variables._1 -> testPoint._1, variables._2 -> testPoint._2)
        //replace the x and y vars with the values of testPoint then evaluate the two expressions with the JSengine.
        // the two sides should be equal
        engine.eval(formatExpression(lhs, variableValues)) == engine.eval(formatExpression(rhs, variableValues))
      } else false)
    } else false
  }
  def isCorrect(responseValue:String):Boolean = compareEquations(responseValue)
  def isValueCorrect(v: String, index: Option[Int]) = compareEquations(v)
}
object CorrectResponseEquation{
  val engine = new ScriptEngineManager().getEngineByName("JavaScript")
  def apply(node:Node):CorrectResponseEquation = {
    if ((node \ "value").size != 1) {
      throw new RuntimeException("Cardinality is set to single but there is not one <value> declared: " + (node \ "value").toString)
    }
    else {
      CorrectResponseEquation((node \ "value").text)
    }
  }
}
case class CorrectResponseSingle(value: String) extends CorrectResponse {
  def isCorrect(responseValue: String): Boolean = responseValue == value

  def isValueCorrect(v: String, index: Option[Int]) = v == value
}

object CorrectResponseSingle {
  def apply(node: Node): CorrectResponseSingle = {

    if ((node \ "value").size != 1) {
      throw new RuntimeException("Cardinality is set to single but there is not one <value> declared: " + (node \ "value").toString)
    }
    else {
      CorrectResponseSingle((node \ "value").text)
    }
  }
}


case class CorrectResponseMultiple(value: Seq[String]) extends CorrectResponse {
  def isCorrect(responseValue: String) = {
    val responseList = responseValue.split(",").toList
    value.sortWith(_ < _) == responseList.sortWith(_ < _)
  }

  def isPartOfCorrect(responseValue: String):Boolean = {
    val responseList = responseValue.split(",").toList
    responseList.foldLeft(true)((acc,r)=>acc && value.contains(r))
  }

  def isValueCorrect(v: String, index: Option[Int]) = value.contains(v)

}

object CorrectResponseMultiple {
  def apply(node: Node): CorrectResponseMultiple = CorrectResponseMultiple(
    (node \ "value").map(_.text)
  )
}

case class CorrectResponseAny(value: Seq[String]) extends CorrectResponse {
  def isCorrect(responseValue: String) = value.find(_ == responseValue).isDefined
  def isValueCorrect(v: String, index: Option[Int]) = value.contains(v)
}
object CorrectResponseAny {
  def apply(node: Node): CorrectResponseAny = CorrectResponseAny((node \ "value").map(_.text))
}

case class CorrectResponseOrdered(value: Seq[String]) extends CorrectResponse {
  def isCorrect(responseValue: String) = {
    val responseList = responseValue.split(",").toList
    value == responseList
  }

  def isValueCorrect(v: String, index: Option[Int]) = {
    index match {
      case Some(i) => value.length > i && value(i) == v
      case None => false
    }
  }
}

object CorrectResponseOrdered {
  def apply(node: Node): CorrectResponseOrdered = CorrectResponseOrdered(
    (node \ "value").map(_.text)
  )
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
      acc + ((node \ "@mapKey").text -> (node \ "@mappedValue").text.toFloat)
    )
    Mapping(mapEntries, defaultValue)
  }
}

case class ItemBody(interactions: Seq[Interaction], feedbackBlocks: Seq[FeedbackInline]) {

  def getInteraction(id: String): Option[Interaction] = interactions.find(_.responseIdentifier == id)
}

object ItemBody {
  def apply(node: Node): ItemBody = {

    val feedbackBlocks = buildTypes[FeedbackInline](node, Seq(
      ("feedbackBlock", FeedbackInline(_, None))
    ))
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

