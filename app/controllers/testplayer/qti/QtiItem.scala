package controllers.testplayer.qti

import scala.xml.Node
import models.ItemResponse
import controllers.Log

class QtiItem(rootNode: Node) {

  private val responseDeclarations = (rootNode \\ "responseDeclaration").map(new ResponseDeclaration(_))
  private val outcomeDeclarations = (rootNode \\ "outcomeDeclaration").map(new OutcomeDeclaration(_))
  private val feedbackInlines = (rootNode \\ "feedbackInline").map(new FeedbackInline(_))
  private val modalFeedbacks = (rootNode \\ "modalFeedback").map(new ModalFeedback(_))

  /**
   * Constructs a Map keyed by responseIdentifier and then by choice identifier to look up appropriate feedback element
   */
  private lazy val responseToFeedbackMap: Map[String, Map[String, FeedbackElement]] = {
    optMap[String, Map[String, FeedbackElement]](
      (rootNode \\ "choiceInteraction").map(responseIdentifierNode => {
        ((responseIdentifierNode \ "@responseIdentifier").text ->
          optMap[String, FeedbackElement]((responseIdentifierNode \\ "feedbackInline").map(feedbackNode => {
            (feedbackNode \ "@identifier").text -> new FeedbackInline(feedbackNode)
          })).getOrElse(Map[String, FeedbackElement]()))
      })).getOrElse(Map[String, Map[String, FeedbackElement]]())
  }

  private def responseForIdentifier(responseIdentifier: String): Option[ResponseDeclaration] =
    responseDeclarations.find(_.identifier.equals(responseIdentifier))

  /**
   * Returns a Map of outcomeIdentifiers and their matching values. For now, this will only be something
   * Map(SCORE -> 1) and Map(SCORE -> 0) for incorrect responses.
   **/
  private def processResponse(responseIdentifier: String, choiceIdentifier: String): Map[String, String] = {

    var response = defaultOutcome
    responseForIdentifier(responseIdentifier) match {
      case Some(responseDeclaration: ResponseDeclaration) =>
        response += ("SCORE" -> responseDeclaration.responseFor(choiceIdentifier))
      case None => {}
    }
    response
  }

  private def processResponse(responseIdentifier: String, choiceIdentifiers: Seq[String]): Map[String, String] = {
    var response = defaultOutcome
    responseForIdentifier(responseIdentifier) match {
      case Some(responseDeclaration: ResponseDeclaration) => {
        response += ("SCORE" -> responseDeclaration.responseFor(choiceIdentifiers))
      }
      case None => {}
    }
    response
  }

  private def matchesSimple(responseIdentifier: String, choiceIdentifier: String): Option[FeedbackElement] =
    responseForIdentifier(responseIdentifier).find(_.responseFor(choiceIdentifier) equals "1") match {
      case Some(responseDeclaration) => getBasicFeedbackMatch(responseIdentifier, choiceIdentifier)
      case None => None
    }

  private def defaultOutcome: Map[String, String] =
    outcomeDeclarations.map(outcomeDeclaration => (outcomeDeclaration.identifier, outcomeDeclaration.value)).toMap

  def feedback(itemResponses: Seq[ItemResponse]): Seq[FeedbackElement] = {
    itemResponses.map(feedback(_)).flatten
  }

  def feedback(itemResponse: ItemResponse): Seq[FeedbackElement] = {
    feedback(itemResponse.id, itemResponse.value)
  }

  def feedback(responseIdentifier: String, choiceIdentifier: String): Seq[FeedbackElement] = {
    matchesSimple(responseIdentifier, choiceIdentifier)
    if (choiceIdentifier.contains(",")) {
      feedback(responseIdentifier, choiceIdentifier.split(','))
    }
    else {
      List(
        List[Option[FeedbackElement]](getBasicFeedbackMatch(responseIdentifier, choiceIdentifier)).flatten,
        getFeedbackForResponse(processResponse(responseIdentifier, choiceIdentifier))
      ).flatten
    }
  }

  def feedback(responseIdentifier: String, choiceIdentifiers: Seq[String]): Seq[FeedbackElement] =
    getFeedbackForResponse(processResponse(responseIdentifier, choiceIdentifiers))

  private def getFeedbackForResponse(responses: Map[String, String]): Seq[FeedbackElement] = {
    (feedbackInlines ++ modalFeedbacks).filter(
      feedback =>
        responses.contains(feedback.outcomeIdentifier) &&
          (responses.getOrElse(feedback.outcomeIdentifier, "") equals feedback.identifier)
    )
  }

  def getBasicFeedbackMatch(responseIdentifier: String, identifier: String): Option[FeedbackElement] = {
    responseForIdentifier(responseIdentifier) match {
      case Some(responseDeclaration: ResponseDeclaration) => {
        if (responseDeclaration.responseFor(identifier) equals "1") {
          Some(responseToFeedbackMap.get(responseIdentifier).get(identifier))
        }
        else None
      }
      case None => None
    }
  }

  // Translates a collection of tuples to an Option[Map]
  private def optMap[A,B](in: Iterable[(A,B)]): Option[Map[A,B]] =
    in.iterator.foldLeft(Option(Map[A,B]())) {
      case (Some(m),e @ (k,v)) if m.getOrElse(k, v) == v => Some(m + e)
      case _ => None
  }
}