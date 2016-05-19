package org.corespring.v2.api.services

import org.bson.types.ObjectId
import org.corespring.container.components.outcome.ScoreProcessor
import org.corespring.container.components.response.OutcomeProcessor
import org.corespring.models.{ Standard, Subject }
import org.corespring.models.item.{ FieldValue, PlayerDefinition }
import org.corespring.models.json.JsonFormatting
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }

import scala.concurrent.ExecutionContext
import scalaz.Success

class BasicScoreServiceTest extends Specification with Mockito {

  val jsonFormatting = new JsonFormatting {
    override def fieldValue: FieldValue = ???

    override def findStandardByDotNotation: (String) => Option[Standard] = ???

    override def rootOrgId: ObjectId = ???

    override def findSubjectById: (ObjectId) => Option[Subject] = ???
  }

  import jsonFormatting.formatPlayerDefinition

  class scoreScope extends Scope {

    val playerDefinition = PlayerDefinition(
      files = Seq.empty,
      xhtml = "<html/>",
      components = Json.obj(),
      summaryFeedback = "",
      customScoring = None)

    val answers = Json.obj("these" -> "are", "the" -> "answers")
    val outcome = Json.obj("this" -> "is", "the" -> "outcome")
    val score = Json.obj("this" -> "is", "the" -> "score")

    lazy val outcomeProcessor = mock[OutcomeProcessor]
    outcomeProcessor.createOutcome(Json.toJson(playerDefinition), Json.obj("components" -> answers), Json.obj()) returns outcome

    lazy val scoreProcessor = {
      val m = mock[ScoreProcessor]
      m.score(any[JsValue], any[JsValue], any[JsValue]) returns score
      m
    }
    lazy val context = ScoreServiceExecutionContext(ExecutionContext.global)
    lazy val service = new BasicScoreService(outcomeProcessor, scoreProcessor, context)
    val response = service.score(playerDefinition, answers)
  }

  "score" should {

    "call ScoreProcessor with itemJson, componentAnswers, and outcome" in new scoreScope {
      there was one(scoreProcessor).score(Json.toJson(playerDefinition), Json.obj("components" -> answers), outcome)
    }

    "return score from ScoreProcessor with success" in new scoreScope {
      response must be equalTo (Success(score))
    }

  }
}
