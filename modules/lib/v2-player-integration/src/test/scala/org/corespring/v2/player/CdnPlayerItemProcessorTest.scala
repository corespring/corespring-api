package org.corespring.v2.player

import org.bson.types.ObjectId
import org.corespring.models.item.resource.{ StoredFile, BaseFile }

import org.corespring.models.json.item.PlayerDefinitionFormat
import org.corespring.models.{ Standard, Subject }
import org.corespring.models.item.PlayerDefinition
import org.corespring.models.item.FieldValue
import org.corespring.models.json.JsonFormatting
import org.corespring.v2.player.cdn.ItemAssetResolver
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsString, JsValue, Json }

class CdnPlayerItemProcessorTest extends Specification with Mockito {

  "makePlayerDefinitionJson" should {

    trait scope extends Scope {

      val mockJsonFormatting = {
        val m = mock[JsonFormatting]
        m.formatPlayerDefinition returns PlayerDefinitionFormat
        m
      }

      val mockItemAssetResolver = {
        val m = mock[ItemAssetResolver]
        m.resolve(any[String])(any[String]) returns "//CDN/file"
        m
      }

      val sut = new CdnPlayerItemProcessor(mockItemAssetResolver, mockJsonFormatting)

      def session = Json.obj("id" -> "sessionId", "itemId" -> "itemId")

      def playerDefinition = Some(PlayerDefinition(
        files = Seq(StoredFile("image.jpg", "image/jpeg")),
        xhtml = "<img src=\"image.jpg\"></img>",
        components = Json.obj("model" -> Json.obj("answer" -> "<img src=\"image.jpg\"></img>")),
        summaryFeedback = "this is some text with an image <img src=\"image.jpg\"></img>",
        customScoring = None))

      def unresolvedPlayerDefinitionJson = Json.parse(
        """{
        "xhtml":"<img src=\"image.jpg\"></img>",
        "components":{"model":{"answer":"<img src=\"image.jpg\"></img>"}},
        "summaryFeedback":"this is some text with an image <img src=\"image.jpg\"></img>"
        }""")

    }

    "replace url in xhtml" in new scope {
      val json = sut.makePlayerDefinitionJson(session, playerDefinition)
      (json \ "xhtml").as[String] must_== "<img src=\"//CDN/file\"></img>"
    }

    "replace url in summaryFeedback" in new scope {
      val json = sut.makePlayerDefinitionJson(session, playerDefinition)
      (json \ "summaryFeedback").as[String] must_== "this is some text with an image <img src=\"//CDN/file\"></img>"
    }

    "replace url in components" in new scope {
      val json = sut.makePlayerDefinitionJson(session, playerDefinition)
      (json \ "components" \ "model" \ "answer").as[String] must_== "<img src=\"//CDN/file\"></img>"
    }

    "fail if playerDefinition is not defined" in new scope {
      sut.makePlayerDefinitionJson(session, None) should throwA[IllegalArgumentException]
    }

    "not replace urls if session does not contain an itemId" in new scope {
      val withoutItemId = session - "itemId"
      sut.makePlayerDefinitionJson(withoutItemId, playerDefinition) must_== unresolvedPlayerDefinitionJson
    }

    "not replace urls if files is empty" in new scope {
      val pd = playerDefinition.get
      val emptyFiles = Some(PlayerDefinition(
        files = Seq.empty,
        xhtml = pd.xhtml,
        components = pd.components,
        summaryFeedback = pd.summaryFeedback,
        customScoring = pd.customScoring))
      sut.makePlayerDefinitionJson(session, emptyFiles) must_== unresolvedPlayerDefinitionJson
    }
  }
}
