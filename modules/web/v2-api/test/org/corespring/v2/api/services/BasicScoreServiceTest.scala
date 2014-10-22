package org.corespring.v2.api.services

import org.corespring.container.components.outcome.ScoreProcessor
import org.corespring.container.components.response.OutcomeProcessor
import org.corespring.platform.core.models.item.PlayerDefinition
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{JsValue, Json}
import scalaz.Success

class BasicScoreServiceTest extends Specification with Mockito {

  class scoreScope extends Scope {

    lazy val outcomeProcessor = mock[OutcomeProcessor]
    lazy val scoreProcessor = {
      val m = mock[ScoreProcessor]
      m.score(any[JsValue], any[JsValue], any[JsValue]) returns Json.obj()
      m
    }
    lazy val service = new BasicScoreService(outcomeProcessor, scoreProcessor)
  }

  "BasicScoreService" should {

    "work" in new scoreScope() {
      val playerDefinition =  PlayerDefinition(
          files = Seq.empty,
          xhtml = "<html/>",
          components = Json.obj(),
          summaryFeedback = "",
          customScoring = None)
      service.score(playerDefinition, Json.obj()) must_== Success(Json.obj())
    }
  }
}
