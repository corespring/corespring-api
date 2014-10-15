package org.corespring.v2.api.services

import org.corespring.container.components.outcome.ScoreProcessor
import org.corespring.container.components.response.OutcomeProcessor
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class BasicScoreServiceTest extends Specification with Mockito {

  class scoreScope extends Scope {

    lazy val outcomeProcessor = mock[OutcomeProcessor]
    lazy val scoreProcessor = mock[ScoreProcessor]
    lazy val service = new BasicScoreService(outcomeProcessor, scoreProcessor)
  }

  "BasicScoreService" should {
    "score" in new scoreScope() {
      true === true
    }
  }
}
