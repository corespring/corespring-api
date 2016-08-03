package org.corespring.v2.player.cdn

import org.corespring.models.item.PlayerDefinition
import org.corespring.models.item.resource.StoredFile
import org.corespring.models.json.JsonFormatting
import org.corespring.models.json.item.PlayerDefinitionFormat
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsString, Json }

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
        m.resolve("itemId")("FigurePattern2.png") returns "//CDN/FigurePattern2.png"
        m.resolve("itemId")("Pattern2.png") returns "//CDN/Pattern2.png"
        m
      }

      val sut = new CdnPlayerItemProcessor(mockItemAssetResolver, mockJsonFormatting)

      def session = Json.obj("id" -> "sessionId", "itemId" -> "itemId")

      def playerDefinition = Some(PlayerDefinition(
        files = Seq(StoredFile("FigurePattern2.png", "image/png")),
        xhtml = "<img src=\"FigurePattern2.png\"></img>",
        components = Json.obj(
          "1" -> Json.obj("model" -> Json.obj("answer" -> "<img src=\"FigurePattern2.png\"></img>")),
          "2" -> Json.obj("fileName" -> "FigurePattern2.png")
        ),
        summaryFeedback = "this is some text with an image <img src=\"FigurePattern2.png\"></img>",
        customScoring = None,
        config = Json.obj()))

      def unresolvedPlayerDefinitionJson = Json.parse(
        """{
        "xhtml":"<img src=\"FigurePattern2.png\"></img>",
        "components": {
          "1": {"model":{"answer":"<img src=\"FigurePattern2.png\"></img>"}},
          "2": {"fileName": "FigurePattern2.png"}
        },
        "summaryFeedback":"this is some text with an image <img src=\"FigurePattern2.png\"></img>"
        }""")

    }

    "replace url in xhtml" in new scope {
      val json = sut.makePlayerDefinitionJson(session, playerDefinition)
      (json \ "xhtml").as[String] must_== "<img src=\"//CDN/FigurePattern2.png\"></img>"
    }

    "replace url in summaryFeedback" in new scope {
      val json = sut.makePlayerDefinitionJson(session, playerDefinition)
      (json \ "summaryFeedback").as[String] must_== "this is some text with an image <img src=\"//CDN/FigurePattern2.png\"></img>"
    }

    "replace url in xml in component model" in new scope {
      val json = sut.makePlayerDefinitionJson(session, playerDefinition)
      (json \ "components" \ "1" \ "model" \ "answer").as[String] must_== "<img src=\"//CDN/FigurePattern2.png\"></img>"
    }

    "replace url in value of component model" in new scope {
      val json = sut.makePlayerDefinitionJson(session, playerDefinition)
      (json \ "components" \ "2" \ "fileName").as[String] must_== "//CDN/FigurePattern2.png"
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
        customScoring = pd.customScoring,
        config = pd.config))
      sut.makePlayerDefinitionJson(session, emptyFiles) must_== unresolvedPlayerDefinitionJson
    }

    "not replace an image twice if its name is contained in an already replaced one" in new scope {
      val pd = playerDefinition.get
      val files = Seq(StoredFile("FigurePattern2.png", "image/png"), StoredFile("Pattern2.png", "image/png"))
      val xhtml = "<img src=\"FigurePattern2.png\"></img> <img src=\"Pattern2.png\"></img>"
      val multipleFiles = Some(PlayerDefinition(
        files = files,
        xhtml = xhtml,
        components = pd.components,
        summaryFeedback = pd.summaryFeedback,
        customScoring = pd.customScoring,
        config = pd.config))
      val jsonResult = sut.makePlayerDefinitionJson(session, multipleFiles)
      (jsonResult \ "xhtml") must_== JsString("<img src=\"//CDN/FigurePattern2.png\"></img> <img src=\"//CDN/Pattern2.png\"></img>")
    }

  }
}
