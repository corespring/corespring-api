package controllers.testplayer.qti

import scala.xml.Node

class QtiItem(rootNode: Node) {

  private val responseDeclarations = (rootNode \\ "responseDeclaration").map(new ResponseDeclaration(_))
  private val outcomeDeclarations = (rootNode \\ "outcomeDeclaration").map(new OutcomeDeclaration(_))
  private val feedbackInlines = (rootNode \\ "feedbackInline").map(new FeedbackInline(_))
  private val modalFeedbacks = (rootNode \\ "modalFeedback").map(new ModalFeedback(_))

  private def responseForIdentifier(responseIdentifier: String): Option[ResponseDeclaration] =
    responseDeclarations.find(_.identifier.equals(responseIdentifier))

  /**
   * Returns a Map of outcomeIdentifiers and their matching values. For now, this will only be something
   * Map(SCORE -> 1) and Map(SCORE -> 0) for incorrect responses.
   **/
  private def processResponse(responseIdentifier: String, choiceIdentifier: String): Map[String, String] = {
    var response = defaultOutcome
    responseForIdentifier(responseIdentifier) match {
      case Some(responseDeclaration: ResponseDeclaration) => {
        response += ("SCORE" -> responseDeclaration.responseFor(choiceIdentifier))
      }
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

  private def defaultOutcome: Map[String, String] =
    outcomeDeclarations.map(outcomeDeclaration => (outcomeDeclaration.identifier, outcomeDeclaration.value)).toMap

  def feedback(responseIdentifier: String, choiceIdentifier: String): Seq[FeedbackElement] = {
    if (choiceIdentifier.contains(",")) feedback(responseIdentifier, choiceIdentifier.split(','))
      else getFeedbackForResponse(processResponse(responseIdentifier, choiceIdentifier))
  }

  def feedback(responseIdentifier: String, choiceIdentifiers: Seq[String]): Seq[FeedbackElement] =
    getFeedbackForResponse(processResponse(responseIdentifier, choiceIdentifiers))

  private def getFeedbackForResponse(responses: Map[String, String]): Seq[FeedbackElement] =
    (feedbackInlines ++ modalFeedbacks).filter(
      feedback => {
        responses.contains(feedback.outcomeIdentifier) &&
          (responses.getOrElse(feedback.outcomeIdentifier, "") equals feedback.identifier)
      }
    )

}