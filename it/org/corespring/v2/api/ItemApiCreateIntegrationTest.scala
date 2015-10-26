package org.corespring.v2.api

import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.ItemHelper
import org.corespring.it.scopes.orgWithAccessToken
import org.corespring.models.item.FieldValue
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.execute.Result
import org.specs2.matcher.MatchResult
import play.api.http.Writeable
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ AnyContent, AnyContentAsEmpty, AnyContentAsJson }
import play.api.test.{ FakeHeaders, FakeRequest }

class ItemApiCreateIntegrationTest extends IntegrationSpecification {

  val routes = org.corespring.v2.api.routes.ItemApi

  "V2 - ItemApi" should {
    "create" should {

      def assertCall[A](r: FakeRequest[A], expectedStatus: Int = OK)(implicit wr: Writeable[A]): MatchResult[Any] = {
        route(r).map { result =>

          if (status(result) == OK) {
            val id = (contentAsJson(result) \ "id").asOpt[String]
            ItemHelper.delete(VersionedId(id.get).get)
          }
          status(result) === expectedStatus
        }.getOrElse(ko("no route found"))
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

        bootstrap.Main.fieldValueService.insert(FieldValue())
        assertCall(
          createRequest[AnyContentAsJson](s"access_token=$accessToken", Some("application/json"), Some(Json.obj())),
          OK)
      }

    }
  }
}
