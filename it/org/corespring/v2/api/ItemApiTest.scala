package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.item.{ Item, PlayerDefinition }
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.SecureSocialHelpers
import org.corespring.test.helpers.models.{ V2SessionHelper, ItemHelper }
import org.corespring.v2.player.scopes.{ orgWithAccessTokenAndItem, user, orgWithAccessToken }
import play.api.http.Writeable
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ AnyContent, AnyContentAsEmpty, AnyContentAsJson }
import play.api.test.{ FakeHeaders, FakeRequest }

class ItemApiTest extends IntegrationSpecification {

  val routes = org.corespring.v2.api.routes.ItemApi

  "V2 - ItemApi" should {
    "create" should {

      def assertCall[A](r: FakeRequest[A], expectedStatus: Int = OK)(implicit wr: Writeable[A]) = {
        route(r).map { result =>

          if (status(result) == OK) {
            val id = (contentAsJson(result) \ "id").as[String]
            ItemHelper.delete(VersionedId(id).get)
          }

          status(result) === expectedStatus
        }.getOrElse(failure("no route found"))
      }

      def createRequest[B <: AnyContent](query: String = "", contentTypeHeader: Option[String] = None, json: Option[JsValue] = None): FakeRequest[B] = {
        val r: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
          routes.create.method,
          if (query.isEmpty) routes.create.url else s"${routes.create.url}?$query",
          FakeHeaders(),
          AnyContentAsEmpty)

        val withHeader: FakeRequest[B] = contentTypeHeader.map(ct => r.withHeaders(CONTENT_TYPE -> ct)).getOrElse(r).asInstanceOf[FakeRequest[B]]
        val out: FakeRequest[B] = json.map(j => withHeader.withJsonBody(j)).getOrElse(withHeader).asInstanceOf[FakeRequest[B]]
        out
      }

      s"$UNAUTHORIZED - for plain request" in {
        val r: FakeRequest[AnyContentAsEmpty.type] = createRequest[AnyContentAsEmpty.type]()
        assertCall(r, UNAUTHORIZED)
      }

      s"$BAD_REQUEST - for token based request with no json header" in new orgWithAccessToken {
        assertCall(createRequest[AnyContentAsEmpty.type](s"access_token=$accessToken"), BAD_REQUEST)
      }

      s"$BAD_REQUEST - for token based request with json header - but no json body" in new orgWithAccessToken {
        assertCall(
          createRequest[AnyContentAsEmpty.type](s"access_token=$accessToken", Some("application/json")),
          BAD_REQUEST)
      }

      s"$OK - for token based request with json header - with json body" in new orgWithAccessToken {
        assertCall(
          createRequest[AnyContentAsJson](s"access_token=$accessToken", Some("application/json"), Some(Json.obj())),
          OK)
      }

    }

    trait checkScore extends orgWithAccessTokenAndItem {

      val update = ItemServiceWired.findOneById(itemId).get.copy(playerDefinition = Some(
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

    "check score" should {

      s"$OK - with multiple choice that is correct" in new checkScore {
        val answers = Json.obj("components" -> Json.obj("1" -> Json.obj("answers" -> Json.arr("carrot"))))
        result.map { r =>
          val resultString = s"""{"summary":{"maxPoints":1,"points":1.0,"percentage":100.0},"components":{"1":{"weight":1,"score":1.0,"weightedScore":1.0}}}"""
          val resultJson = Json.parse(resultString)
          status(r) === OK
          contentAsJson(r) === resultJson
        }.getOrElse(failure("didn't load result"))
      }

      s"$OK - with multiple choice that is incorrect" in new checkScore {

        val answers = Json.obj("components" -> Json.obj("1" -> Json.obj("answers" -> Json.arr("banana"))))

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
