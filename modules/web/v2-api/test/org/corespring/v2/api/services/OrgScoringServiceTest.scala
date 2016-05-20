package org.corespring.v2.api.services

import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.models.item.PlayerDefinition
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.PlayerDefinitionService
import org.corespring.v2.auth.models.MockFactory
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.sessiondb.SessionService
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Failure, Success }

class OrgScoringServiceTest extends Specification with Mockito {

  trait scope extends Scope with MockFactory {

    lazy val item = mockItem
    lazy val vid = item.id

    lazy val orgAndOpts = mockOrgAndOpts()

    lazy val sessionService = {
      val m = mock[SessionService]
      m.loadMultipleTwo(any[Seq[String]]) returns Future.successful(Nil)
      m
    }

    lazy val playerDefinitionService = {
      val m = mock[PlayerDefinitionService]
      m.findMultiplePlayerDefinitions(any[ObjectId], any[VersionedId[ObjectId]]) returns {
        Future.successful(Nil)
      }
      m
    }

    lazy val scoreService = mock[ScoreService]
    lazy val scoringServiceExecutionContext = new OrgScoringExecutionContext(ExecutionContext.global)
    lazy val service = new OrgScoringService(
      sessionService,
      playerDefinitionService,
      scoreService,
      scoringServiceExecutionContext)
  }

  trait base extends scope {
    val json = Json.obj("itemId" -> vid.toString, "_id" -> Json.obj("$oid" -> "sessionId"))
    lazy val score = Json.obj("score" -> 1)

    sessionService.loadMultipleTwo(any[Seq[String]]) returns Future.successful(Seq("sessionId" -> Some(json)))
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
      sessionService.loadMultipleTwo(any[Seq[String]]) returns Future.successful(Nil)
      playerDefinitionService.findMultiplePlayerDefinitions(any[ObjectId], any[VersionedId[ObjectId]]) returns Future.successful(Nil)
      scoreService.scoreMultiple(any[PlayerDefinition], any[Seq[JsValue]]) returns Nil
      service.scoreMultipleSessions(orgAndOpts)(Nil) must equalTo(Nil).await
    }

    "return errors for missing sessions" in new scoreMultipleSessions {
      sessionService.loadMultipleTwo(any[Seq[String]]) returns Future.successful(Seq("missing-sessionId" -> None))
      scoreService.scoreMultiple(any[PlayerDefinition], any[Seq[JsValue]]) returns Nil
      val f = service.scoreMultipleSessions(orgAndOpts)(Seq("missing-sessionId"))
      f must equalTo(Seq(ScoreResult("missing-sessionId", Failure(generalError("No session found"))))).await
    }

    "return errors for missing itemId in session" in new scoreMultipleSessions {
      scoreService.scoreMultiple(any[PlayerDefinition], any[Seq[JsValue]]) returns Nil
      sessionService.loadMultipleTwo(any[Seq[String]]) returns Future.successful(Seq("sessionId" -> Some(Json.obj())))
      val f = service.scoreMultipleSessions(orgAndOpts)(Seq("sessionId"))
      f must equalTo(Seq(ScoreResult("sessionId", Failure(generalError("No item id"))))).await
    }

    "return errors for bad itemId in session" in new scoreMultipleSessions {
      scoreService.scoreMultiple(any[PlayerDefinition], any[Seq[JsValue]]) returns Nil
      sessionService.loadMultipleTwo(any[Seq[String]]) returns Future.successful(Seq("sessionId" -> Some(Json.obj("itemId" -> "bad"))))
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
  }
}
