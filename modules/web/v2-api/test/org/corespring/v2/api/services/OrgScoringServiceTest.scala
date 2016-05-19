package org.corespring.v2.api.services

import org.corespring.services.item.PlayerDefinitionService
import org.corespring.v2.auth.models.MockFactory
import org.corespring.v2.sessiondb.SessionService
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.ExecutionContext

class OrgScoringServiceTest extends Specification with Mockito {

  trait scope extends Scope with MockFactory {
    lazy val orgAndOpts = mockOrgAndOpts()

    lazy val sessionService = mock[SessionService]
    lazy val playerDefinitionService = mock[PlayerDefinitionService]
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
        val f = service.scoreMultipleSessions(orgAndOpts)(Seq("missing-sessionId"))
        f must equalTo(Seq()).await
      }

    }

  }

}
