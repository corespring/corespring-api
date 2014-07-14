package org.corespring.v2.api

import org.corespring.it.IntegrationSpecification
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.SecureSocialHelpers
import org.corespring.test.helpers.models.ItemHelper
import org.corespring.v2.player.scopes.{ user, orgWithAccessToken }
import play.api.http.Writeable
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ AnyContent, AnyContentAsEmpty, AnyContentAsJson }
import play.api.test.{ FakeHeaders, FakeRequest }

class ItemApiTest extends IntegrationSpecification {

  val create = org.corespring.api.v2.routes.ItemApi.create()

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
          create.method,
          if (query.isEmpty) create.url else s"${create.url}?$query",
          FakeHeaders(),
          AnyContentAsEmpty)

        val withHeader: FakeRequest[B] = contentTypeHeader.map(ct => r.withHeaders(CONTENT_TYPE -> ct)).getOrElse(r).asInstanceOf[FakeRequest[B]]
        val out: FakeRequest[B] = json.map(j => withHeader.withJsonBody(j)).getOrElse(withHeader).asInstanceOf[FakeRequest[B]]
        out
      }

      s"$BAD_REQUEST - for plain request" in {
        val r: FakeRequest[AnyContentAsEmpty.type] = createRequest[AnyContentAsEmpty.type]()
        assertCall(r, BAD_REQUEST)
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

      s"$OK - for session based request" in new user with SecureSocialHelpers {

        val cookie = secureSocialCookie(Some(user)).get

        assertCall(
          createRequest[AnyContentAsJson](contentTypeHeader = Some("application/json"), json = Some(Json.obj())).withCookies(cookie),
          OK)
      }
    }
  }

}
