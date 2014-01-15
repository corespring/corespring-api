package org.corespring.v2player.integration.actionBuilders

import org.bson.types.ObjectId
import org.corespring.container.client.actions.PlayerJsRequest
import org.corespring.platform.core.services.UserService
import org.corespring.test.PlaySingleton
import org.specs2.matcher.{Expectable, Matcher}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.Results._
import play.api.mvc.{SimpleResult, AnyContentAsEmpty, Result, AnyContent}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, FakeHeaders}
import scala.concurrent.Future
import org.corespring.player.accessControl.models.RenderOptions
import org.corespring.player.accessControl.cookies.PlayerCookieKeys

class PlayerLauncherActionBuilderTest extends Specification with Mockito {

  PlaySingleton.start

  val mockOrgId = ObjectId.get

  case class scope(orgId: Option[ObjectId] = Some(mockOrgId), decryptEnabled: Boolean = true) extends Scope {
    val builder = new PlayerLauncherActionBuilder {
      def toOrgId(s: String): Option[ObjectId] = orgId

      def userService: UserService = mock[UserService]

      def decrypt(orgId: ObjectId, encrypted: String): Option[String] = if (decryptEnabled) Some(encrypted) else None
    }

    def handler(r: PlayerJsRequest[AnyContent]): Result = Ok(s"${r.isSecure}")

    def call(path: String): Future[SimpleResult] = builder.playerJs(handler)(FakeRequest("", path, FakeHeaders(), AnyContentAsEmpty))

  }

  case class returnResult(expectedStatus: Int, body: String) extends Matcher[Future[SimpleResult]] {
    def apply[S <: Future[SimpleResult]](s: Expectable[S]) = {
      val actualStatus = status(s.value)
      val actualBody = contentAsString(s.value)
      result(actualStatus == expectedStatus && actualBody == body,
        s"${actualStatus} matches $expectedStatus & $body",
        s"[$actualStatus:$actualBody] does not match [$expectedStatus:$body]",
        s)
    }
  }

  case class haveCookies(cookies: (String, String)*) extends Matcher[Future[SimpleResult]] {
    def apply[S <: Future[SimpleResult]](s: Expectable[S]) = {
      val actualSession = session(s.value)

      val valueResults: Seq[(String, String)] = cookies.map {
        kv =>
          val valueResult = actualSession.get(kv._1).map {
            v =>
              if (v == kv._2) "equal" else "not equal"
          }.getOrElse("not found")
          (kv._1, valueResult)
      }

      val badResults = valueResults.filterNot(kv => kv._2 == "equal")
      val success = badResults.length == 0

      result(success,
        s"${cookies} matches ${valueResults.mkString(",")}",
        s"${cookies} != ${actualSession.data}",
        s)
    }
  }

  "PlayerLauncherActionBuilder" should {
    import PlayerLauncherActionBuilder.Errors._
    import PlayerCookieKeys._

    "return an error if no apiClient" in new scope {
      call("player.js") must returnResult(BAD_REQUEST, noClientId)
    }

    "return an error if no options" in new scope {
      call("player.js?apiClient={}") must returnResult(BAD_REQUEST, noOptions)
    }

    "return an error if can't retrieve orgId" in new scope(orgId = None) {
      call("player.js?apiClient={}&options={}") must returnResult(BAD_REQUEST, noOrgId)
    }

    "return an error if can't decrypt" in new scope(decryptEnabled = false) {
      call("player.js?apiClient={}&options={}") must returnResult(BAD_REQUEST, cantDecrypt)
    }

    "return an error if can't parse json" in new scope {
      call("player.js?apiClient={}&options=badjson") must returnResult(BAD_REQUEST, badJson)
    }

    "return body if all is ok and secure" in new scope {
      val opts = """{"itemId":"*","sessionId":"*","secure":true}"""
      val path = s"""player.js?apiClient={}&options=$opts"""

      call(path) must returnResult(OK, "true")
      call(path) must haveCookies(ORG_ID -> mockOrgId.toString, RENDER_OPTIONS -> opts)
    }

    "return body if all is ok and not secure" in new scope {
      call( """player.js?apiClient={}&options={"itemId":"*","sessionId":"*","secure":false}""") must returnResult(OK, "false")
    }
  }
}
