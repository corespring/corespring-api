package org.corespring.v2.api

import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.item._
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.v2.player.scopes.orgWithAccessTokenAndItem
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.AnyContentAsJson
import play.api.test.{ FakeHeaders, FakeRequest }

class ItemApiCheckScoreTest extends IntegrationSpecification {

  val routes = org.corespring.v2.api.routes.ItemApi

  "V2 - ItemApi" should {

    "check score" should {

      trait checkScore extends orgWithAccessTokenAndItem {

        val update = ItemServiceWired.findOneById(itemId).get.copy(data = None, playerDefinition = Some(
          PlayerDefinition(
            Seq.empty,
            "html",

            /**
             * Note: there is a risk of adding a data model from an externally defined component,
             * that may change. But it is useful for an integration test to run end-to-end in the system.
             * Is there a better way to guarantee the data model will be up to date?
             */
            Json.obj(
              "1" -> Json.obj(
                "componentType" -> "corespring-multiple-choice",
                "correctResponse" -> Json.obj("value" -> Json.arr("carrot")),
                "model" -> Json.obj(
                  "config" -> Json.obj(
                    "singleChoice" -> true),
                  "prompt" -> "Carrot?",
                  "choices" -> Json.arr(
                    Json.obj("label" -> "carrot", "value" -> "carrot"),
                    Json.obj("label" -> "banana", "value" -> "banana"))))),
            "",
            None)))

        ItemServiceWired.save(update)
        val call = routes.checkScore(itemId.toString)

        def answers: JsValue

        lazy val result = route(FakeRequest(call.method, s"${call.url}?access_token=$accessToken", FakeHeaders(), AnyContentAsJson(answers)))

      }

      s"$OK - with multiple choice that is correct" in new checkScore {
        val answers = Json.obj("1" -> Json.obj("answers" -> Json.arr("carrot")))
        result.map { r =>
          val resultString = s"""{"summary":{"maxPoints":1,"points":1.0,"percentage":100.0},"components":{"1":{"weight":1,"score":1.0,"weightedScore":1.0}}}"""
          val resultJson = Json.parse(resultString)
          status(r) === OK
          contentAsJson(r) === resultJson
        }.getOrElse(failure("didn't load result"))
      }

      s"$OK - with multiple choice that is incorrect" in new checkScore {

        val answers = Json.obj("1" -> Json.obj("answers" -> Json.arr("banana")))

        result.map { r =>
          val resultString = s"""{"summary":{"maxPoints":1,"points":0.0,"percentage":0.0},"components":{"1":{"weight":1,"score":0.0,"weightedScore":0.0}}}"""
          val resultJson = Json.parse(resultString)
          status(r) === OK
          contentAsJson(r) === resultJson
        }.getOrElse(failure("didn't load result"))
      }
    }
  }
}
