package org.corespring.v2.api.services

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.PlayerDefinitionService
import org.corespring.v2.auth.models.MockFactory
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.sessiondb.SessionService
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.Json

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.Failure

class OrgScoringServiceTest extends Specification with Mockito {

  trait scope extends Scope with MockFactory {
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

  "scoreMultipleSessions" should {
    "scoreMultipleSessions" should {
      trait scoreMultipleSessions extends scope {

      }

      "return nil for nil" in new scoreMultipleSessions {
        service.scoreMultipleSessions(orgAndOpts)(Nil) must equalTo(Nil).await
      }

      "return errors for missing sessions" in new scoreMultipleSessions {
        sessionService.loadMultipleTwo(any[Seq[String]]) returns Future.successful(Seq("missing-sessionId" -> None))
        val f = service.scoreMultipleSessions(orgAndOpts)(Seq("missing-sessionId"))
        f must equalTo(Seq(ScoreResult("missing-sessionId", Failure(generalError("No session found"))))).await
      }

      "return errors for missing itemId in session" in new scoreMultipleSessions {
        sessionService.loadMultipleTwo(any[Seq[String]]) returns Future.successful(Seq("sessionId" -> Some(Json.obj())))
        val f = service.scoreMultipleSessions(orgAndOpts)(Seq("sessionId"))
        f must equalTo(Seq(ScoreResult("sessionId", Failure(generalError("No item id"))))).await
      }

      "return errors for bad itemId in session" in new scoreMultipleSessions {
        sessionService.loadMultipleTwo(any[Seq[String]]) returns Future.successful(Seq("sessionId" -> Some(Json.obj("itemId" -> "bad"))))
        val f = service.scoreMultipleSessions(orgAndOpts)(Seq("sessionId"))
        f must equalTo(Seq(ScoreResult("sessionId", Failure(generalError("Bad Item id"))))).await
      }
    }
  }
}
