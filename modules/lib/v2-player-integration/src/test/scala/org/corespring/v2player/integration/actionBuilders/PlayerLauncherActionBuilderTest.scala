package org.corespring.v2player.integration.actionBuilders

import org.bson.types.ObjectId
import org.corespring.container.client.actions.PlayerJsRequest
import org.corespring.platform.core.services.UserService
import org.corespring.player.accessControl.cookies.PlayerCookieKeys
import org.corespring.test.PlaySingleton
import org.corespring.test.matchers.RequestMatchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.Some
import scala.concurrent.Future
import org.corespring.v2player.integration.securesocial.SecureSocialService

class PlayerLauncherActionBuilderTest
  extends Specification
  with Mockito
  with RequestMatchers {

  PlaySingleton.start

  val mockOrgId = ObjectId.get

  def mockSecureSocial = {
    val out = mock[SecureSocialService]
    out.currentUser(any[Request[AnyContent]]) returns None
    out
  }

  case class scope(orgId: Option[ObjectId] = Some(mockOrgId), decryptEnabled: Boolean = true) extends Scope {
    val builder = new PlayerLauncherActionBuilder(mockSecureSocial, mock[UserService]) {
      def toOrgId(s: String): Option[ObjectId] = orgId

      def decrypt(request: Request[AnyContent], orgId: ObjectId, encrypted: String): Option[String] = if (decryptEnabled) Some(encrypted) else None
    }

    def handler(r: PlayerJsRequest[AnyContent]): Result = Ok(s"${r.isSecure}")

    def call(path: String): Future[SimpleResult] = builder.playerJs(handler)(FakeRequest("", path, FakeHeaders(), AnyContentAsEmpty))
  }

  "PlayerLauncherActionBuilder" should {
    import PlayerCookieKeys._
    import PlayerLauncherActionBuilder.Errors._

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
