package org.corespring.v2.api.services

import org.bson.types.ObjectId
import org.corespring.container.components.outcome.ScoreProcessor
import org.corespring.container.components.response.OutcomeProcessor
import org.corespring.models.item.{ FieldValue, PlayerDefinition }
import org.corespring.models.json.JsonFormatting
import org.corespring.models.{ Standard, Subject }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }
import play.api.test.PlaySpecification

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.Success

class BasicScoreServiceTest extends Specification with Mockito with PlaySpecification {

  import ExecutionContext.Implicits.global

  val jsonFormatting = new JsonFormatting {
    override def fieldValue: FieldValue = ???

    override def findStandardByDotNotation: (String) => Option[Standard] = ???

    override def rootOrgId: ObjectId = ???

    override def findSubjectById: (ObjectId) => Option[Subject] = ???
  }

  import Json._
  import jsonFormatting.formatPlayerDefinition

  trait scope extends Scope {

    val playerDefinition = PlayerDefinition.empty

    lazy val playerDefJson = toJson(playerDefinition)
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
  }

  "score" should {

    trait score extends scope {
      val response = service.score(playerDefinition, answers)
    }

    "call ScoreProcessor with itemJson, componentAnswers, and outcome" in new score {
      there was one(scoreProcessor).score(toJson(playerDefinition), obj("components" -> answers), outcome)
    }

    "return score from ScoreProcessor with success" in new score {
      response must be equalTo (Success(score))
    }
  }

  "scoreMultiple" should {

    trait scoreMultiple extends scope

    "call ScoreProcessor" in new scoreMultiple {

      val multiple: Seq[JsValue] = (1 to 10).foldRight[Seq[JsValue]](Nil)((_, acc) => acc :+ obj("components" -> answers))
      val response = service.scoreMultiple(playerDefinition, multiple)
      await(Future.sequence(response))
      there was 10.times(scoreProcessor).score(playerDefJson, obj("components" -> answers), outcome)
    }
  }
}
