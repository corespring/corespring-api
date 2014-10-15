package org.corespring.v2.api.services

import org.bson.types.ObjectId
import org.corespring.container.components.outcome.ScoreProcessor
import org.corespring.container.components.response.OutcomeProcessor
import org.corespring.platform.core.models.item.{ PlayerDefinition, Item }
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }

import scalaz.{ Success, Failure }

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
    "return an error if there is no PlayerDefinition" in new scoreScope() {
      val itemId = VersionedId(ObjectId.get)
      service.score(Item(id = itemId), Json.obj()) must_== Failure(service.noPlayerDefinition(itemId))
    }

    "work" in new scoreScope() {
      val item = Item(id = VersionedId(ObjectId.get), playerDefinition = Some(
        PlayerDefinition(
          files = Seq.empty,
          xhtml = "<html/>",
          components = Json.obj(),
          summaryFeedback = "",
          customScoring = None)))
      service.score(item, Json.obj()) must_== Success(Json.obj())
    }
  }
}
