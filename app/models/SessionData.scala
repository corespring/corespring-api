package models

import play.api.libs.json._
import collection.immutable.HashMap
import qti.models._
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import scala.Some
import play.api.libs.json.JsObject
import qti.models.QtiItem.Correctness
import com.codahale.jerkson.{Json => Jerkson}

/**
 * Creates information about the responses.
 * - feedback - feedback to the user about their responses
 * - correctResponses
 *
 * {
 * sessionData: {
 * feedbackContents: {
 * [csFeedbackId]: "[contents of feedback element]",
 * [csFeedbackId]: "[contents of feedback element]"
 * }
 * correctResponse: {
 * irishPresident: "higgins",
 * rainbowColors: ['blue','violet', 'red']
 * }
 * }
 * }
 *
 * TODO: SessionData output should change its output depending on the following:
 * ItemSessionSettings{
 * highlightCorrectResponses,
 * highlightUserResponse,
 * showFeedbackForHighlighted
 * }
 *
 * @param qtiItem
 * @param responses
 */
case class SessionData(qtiItem: QtiItem, responses: Seq[ItemResponse])

object SessionData {

  implicit object SessionDataWrites extends Writes[SessionData] {

    def writes(sd: SessionData) = {

      val correctResponses: List[JsValue] = declarationsToJson(sd.qtiItem.responseDeclarations)

      def filterFeedbacks(feedbacks: Seq[FeedbackInline], displayCorrectResponse: Boolean = false): Seq[FeedbackInline] = {
        var feedbackGroups: HashMap[String, Seq[FeedbackInline]] = HashMap()
        feedbacks.foreach(fi => if (feedbackGroups.get(fi.outcomeIdentifier).isDefined) {
          feedbackGroups += (fi.outcomeIdentifier -> (feedbackGroups.get(fi.outcomeIdentifier).get :+ fi))
        } else {
          feedbackGroups += (fi.outcomeIdentifier -> Seq(fi))
        })
        feedbackGroups.map(kvpair =>
          filterFeedbackGroup(kvpair._1, kvpair._2, displayCorrectResponse)
        ).flatten.toSeq
      }

      /**
       *
       * @param responseIdentifier
       * @param feedbackGroup
       * @param displayCorrectResponse
       * @return
       */
      def filterFeedbackGroup(responseIdentifier: String,
                              feedbackGroup: Seq[FeedbackInline],
                              displayCorrectResponse: Boolean = false): Seq[FeedbackInline] = {


        val responseGroup = sd.responses.filter(ir => ir.id == responseIdentifier) //find the responses corresponding to this feedbackGroup


        /**
         * find if a response element that has the same value as the identifier in the feedbackInline element exists
         */
        def responseAndFeedbackMatch(fi: FeedbackInline): Boolean = {
          responseGroup.find((ir: ItemResponse) => ItemResponse.containsValue(ir, fi.identifier)).isDefined
        }

        //find if the given feedback element represents the correct response
        def isCorrectResponseFeedback(fi: FeedbackInline) = {
          sd.qtiItem.responseDeclarations
            .find(_.identifier == fi.outcomeIdentifier)
            .map(_.isCorrect(fi.identifier) == Correctness.Correct)
            .getOrElse(false)
        }
        val feedbackContents = feedbackGroup.filter(fi => responseAndFeedbackMatch(fi) || (displayCorrectResponse && isCorrectResponseFeedback(fi)))
        if (feedbackContents.isEmpty) {
          feedbackGroup.find(fi => fi.incorrectResponse) match {
            case Some(fi) => Seq(fi)
            case None => Seq()
          }
        } else feedbackContents
      }

      def getFeedbackContent(fi: FeedbackInline) = if (fi.defaultFeedback)
        fi.defaultContent(sd.qtiItem)
      else fi.content

      var feedbackContents: Seq[(String, JsValue)] = Seq()
      sd.qtiItem.itemBody.interactions.foreach(interaction => {
        interaction match {
          case ci: InlineChoiceInteraction => filterFeedbackGroup(ci.responseIdentifier, ci.choices.map(_.feedbackInline).flatten, false)
            .foreach(fi => feedbackContents = feedbackContents :+ (fi.csFeedbackId -> JsString(getFeedbackContent(fi))))
          case ci: ChoiceInteraction => filterFeedbackGroup(ci.responseIdentifier, ci.choices.map(_.feedbackInline).flatten, true)
            .foreach(fi => feedbackContents = feedbackContents :+ (fi.csFeedbackId -> JsString(getFeedbackContent(fi))))
          case oi: OrderInteraction => filterFeedbackGroup(oi.responseIdentifier, oi.choices.map(_.feedbackInline).flatten, true)
            .foreach(fi => feedbackContents = feedbackContents :+ (fi.csFeedbackId -> JsString(getFeedbackContent(fi))))
          case ti: TextEntryInteraction => filterFeedbackGroup(ti.responseIdentifier, ti.feedbackBlocks, true)
            .foreach(fi => feedbackContents = feedbackContents :+ (fi.csFeedbackId -> JsString(getFeedbackContent(fi))))
        }
      })
      filterFeedbacks(sd.qtiItem.itemBody.feedbackBlocks, false).foreach(fi =>
        feedbackContents = feedbackContents :+ (fi.csFeedbackId -> JsString(getFeedbackContent(fi)))
      )


      JsObject(Seq(
        "feedbackContents" -> JsObject(feedbackContents),
        "correctResponses" -> JsArray(correctResponses)
      ))
    }

    /**
     * convert ResponseDeclaration.CorrectResponse -> JsValue
     * @param declarations
     * @return
     */
    private def declarationsToJson(declarations: Seq[ResponseDeclaration]): List[JsValue] = {

      def correctResponseToItemResponse(id: String)(cr: CorrectResponse): ItemResponse = cr match {
        case CorrectResponseSingle(value) => StringItemResponse(id, value)
        case CorrectResponseMultiple(value) => ArrayItemResponse(id, value)
        case CorrectResponseAny(value) => ArrayItemResponse(id, value)
        case CorrectResponseOrdered(value) => ArrayItemResponse(id, value)
        case _ => throw new RuntimeException("Unknown CorrectResponseType: " + cr)
      }

      def _declarationsToItemResponses(declarations: Seq[ResponseDeclaration]): List[ItemResponse] =
      {
        if (declarations.isEmpty) {
          List()
        } else {
          val rd: ResponseDeclaration = declarations.head
          val correctResponseViews = rd.correctResponse.map(correctResponseToItemResponse(rd.identifier))
          correctResponseViews.toList ::: _declarationsToItemResponses(declarations.tail)
        }
      }

      _declarationsToItemResponses(declarations).map(Json.toJson(_))

    }

  }

}


