package org.corespring.v2.api.services

import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.models.{ Standard, Subject }
import org.corespring.models.item.{ FieldValue, PlayerDefinition }
import org.corespring.models.json.JsonFormatting
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.PlayerDefinitionService
import org.corespring.v2.auth.models.MockFactory
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.sessiondb.SessionService
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }
import play.api.test.{ DefaultAwaitTimeout, FutureAwaits }

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Failure, Success }

class OrgScoringServiceTest extends Specification
  with Mockito with FutureAwaits with DefaultAwaitTimeout {

  trait scope extends Scope with MockFactory {

    lazy val sessionId = ObjectId.get
    lazy val item = mockItem
    lazy val vid = item.id

    lazy val orgAndOpts = mockOrgAndOpts()

    lazy val sessionService = {
      val m = mock[SessionService]
      m.loadMultiple(any[Seq[String]]) returns Future.successful(Nil)
      m
    }

    lazy val playerDefinitionService = {
      val m = mock[PlayerDefinitionService]
      m.findMultiplePlayerDefinitions(any[ObjectId], any[VersionedId[ObjectId]]) returns {
        Future.successful(Nil)
      }
      m
    }

    lazy val jsonFormatting = new JsonFormatting {
      override def fieldValue: FieldValue = FieldValue()

      override def findSubjectById: (ObjectId) => Option[Subject] = _ => None

      override def findStandardByDotNotation: (String) => Option[Standard] = _ => None

      override def rootOrgId: ObjectId = ObjectId.get
    }

    lazy val scoreService = mock[ScoreService]
    lazy val scoringServiceExecutionContext = new OrgScoringExecutionContext(ExecutionContext.global)
    lazy val service = new OrgScoringService(
      sessionService,
      playerDefinitionService,
      scoreService,
      scoringServiceExecutionContext,
      jsonFormatting)
  }

  trait base extends scope {
    val json = Json.obj("itemId" -> vid.toString, "_id" -> Json.obj("$oid" -> "sessionId"))
    lazy val score = Json.obj("score" -> 1)

    sessionService.loadMultiple(any[Seq[String]]) returns Future.successful(Seq("sessionId" -> Some(json)))
    playerDefinitionService.findMultiplePlayerDefinitions(any[ObjectId], any[VersionedId[ObjectId]]) returns {
      Future.successful(Seq(vid -> Success(PlayerDefinition.empty)))
    }

    scoreService.scoreMultiple(any[PlayerDefinition], any[Seq[JsValue]]) returns {
      Seq(Future.successful(json -> Success(score)))
    }
  }

  "scoreSession" should {
    trait scoreSession extends base

    "return score" in new scoreSession {
      service.scoreSession(orgAndOpts)("sessionId") must equalTo(ScoreResult("sessionId", Success(score))).await
    }
  }

  "scoreMultipleSessions" should {
    trait scoreMultipleSessions extends base

    "return nil for nil" in new scoreMultipleSessions {
      sessionService.loadMultiple(any[Seq[String]]) returns Future.successful(Nil)
      playerDefinitionService.findMultiplePlayerDefinitions(any[ObjectId], any[VersionedId[ObjectId]]) returns Future.successful(Nil)
      scoreService.scoreMultiple(any[PlayerDefinition], any[Seq[JsValue]]) returns Nil
      service.scoreMultipleSessions(orgAndOpts)(Nil) must equalTo(Nil).await
    }

    "return errors for missing sessions" in new scoreMultipleSessions {
      sessionService.loadMultiple(any[Seq[String]]) returns Future.successful(Seq("missing-sessionId" -> None))
      scoreService.scoreMultiple(any[PlayerDefinition], any[Seq[JsValue]]) returns Nil
      val f = service.scoreMultipleSessions(orgAndOpts)(Seq("missing-sessionId"))
      f must equalTo(Seq(ScoreResult("missing-sessionId", Failure(generalError("No session found"))))).await
    }

    "return errors for missing itemId in session" in new scoreMultipleSessions {
      scoreService.scoreMultiple(any[PlayerDefinition], any[Seq[JsValue]]) returns Nil
      sessionService.loadMultiple(any[Seq[String]]) returns Future.successful(Seq("sessionId" -> Some(Json.obj())))
      val f = service.scoreMultipleSessions(orgAndOpts)(Seq("sessionId"))
      f must equalTo(Seq(ScoreResult("sessionId", Failure(generalError("No item id"))))).await
    }

    "return errors for bad itemId in session" in new scoreMultipleSessions {
      scoreService.scoreMultiple(any[PlayerDefinition], any[Seq[JsValue]]) returns Nil
      sessionService.loadMultiple(any[Seq[String]]) returns Future.successful(Seq("sessionId" -> Some(Json.obj("itemId" -> "bad"))))
      val f = service.scoreMultipleSessions(orgAndOpts)(Seq("sessionId"))
      f must equalTo(Seq(ScoreResult("sessionId", Failure(generalError("Bad Item id"))))).await
    }

    "return error from loading player definition" in new scoreMultipleSessions {

      playerDefinitionService.findMultiplePlayerDefinitions(any[ObjectId], any[VersionedId[ObjectId]]) returns {
        Future.successful(Seq(vid -> Failure(PlatformServiceError("err"))))
      }
      val f = service.scoreMultipleSessions(orgAndOpts)(Seq("sessionId"))
      f must equalTo(Seq(ScoreResult("sessionId", Failure(generalError("err"))))).await
    }

    "return error from scoring" in new scoreMultipleSessions {

      scoreService.scoreMultiple(any[PlayerDefinition], any[Seq[JsValue]]) returns {
        Seq(Future.successful(json -> Failure(generalError("score error"))))
      }

      val f = service.scoreMultipleSessions(orgAndOpts)(Seq("sessionId"))
      f must equalTo(Seq(ScoreResult("sessionId", Failure(generalError("score error"))))).await

    }

    "return scoring" in new scoreMultipleSessions {
      service.scoreMultipleSessions(orgAndOpts)(Seq("sessionId")) must equalTo(Seq(ScoreResult("sessionId", Success(Json.obj("score" -> 1))))).await
    }

    import play.api.libs.json.Json._

    trait scoreWithInlineSession extends base {

      val definition = PlayerDefinition("<h1>test</h1>")

      val inlineSession = obj(
        "_id" -> obj("$oid" -> sessionId.toString),
        "item" -> jsonFormatting.formatPlayerDefinition.writes(definition))

      sessionService.loadMultiple(any[Seq[String]]) returns Future.successful {
        Seq("sessionId" -> Some(inlineSession))
      }

      val scoreResult = service.scoreMultipleSessions(orgAndOpts)(Seq("sessionId"))
    }

    "return scoring for session with inline definition" in new scoreWithInlineSession {
      scoreResult must equalTo(Seq(ScoreResult("sessionId", Success(Json.obj("score" -> 1))))).await
    }

    "call scoreMultiple with inline player definition" in new scoreWithInlineSession {
      await(scoreResult)
      there was one(scoreService).scoreMultiple(PlayerDefinition.empty, Seq(obj()))
    }
  }
}
