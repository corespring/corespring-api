package org.corespring.v2player.integration.actionBuilders

import org.bson.types.ObjectId
import org.corespring.container.client.actions.PlayerJsRequest
import org.corespring.platform.core.services.UserService
import org.corespring.test.PlaySingleton
import org.corespring.test.matchers.RequestMatchers
import org.corespring.v2player.integration.actionBuilders.PlayerLauncherActions._
import org.corespring.v2player.integration.actionBuilders.access.V2PlayerCookieKeys
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.Results._
import play.api.mvc.SimpleResult
import play.api.mvc._
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.Some
import scala.concurrent.Future
import org.corespring.platform.core.controllers.auth.SecureSocialService

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
    val builder = new PlayerLauncherActions(mockSecureSocial, mock[UserService]) {
      def toOrgId(s: String): Option[ObjectId] = orgId

      def decrypt(request: Request[AnyContent], orgId: ObjectId, encrypted: String): Option[String] = if (decryptEnabled) Some(encrypted) else None
    }

    def handler(r: PlayerJsRequest[AnyContent]): Result = r.errors match {
      case Nil => Ok(s"${r.isSecure}")
      case _ => Ok(s"${r.errors.mkString(",")}")
    }

    def call(path: String): Future[SimpleResult] = builder.playerJs(handler)(FakeRequest("", path, FakeHeaders(), AnyContentAsEmpty))
  }

  def returnErrorResult(e: LaunchError) = returnResult(OK, e.message)

  "PlayerLauncherActionBuilder" should {

    "return an error if no apiClient" in new scope {
      call("player.js") must returnErrorResult(noClientId)
    }

    "return an error if no options" in new scope {
      call("player.js?apiClient={}") must returnErrorResult(noOptions)
    }

    "return an error if can't retrieve orgId" in new scope(orgId = None) {
      call("player.js?apiClient={}&options={}") must returnErrorResult(noOrgId)
    }

    "return an error if can't decrypt" in new scope(decryptEnabled = false) {
      call("player.js?apiClient={}&options={}") must returnErrorResult(cantDecrypt)
    }

    "return an error if can't parse json" in new scope {
      call("player.js?apiClient={}&options=badjson") must returnErrorResult(badJson)
    }

    "return body if all is ok and secure" in new scope {
      val opts = """{"itemId":"*","sessionId":"*","secure":true}"""
      val path = s"""player.js?apiClient={}&options=$opts"""

      call(path) must returnResult(OK, "true")
      call(path) must haveCookies(V2PlayerCookieKeys.orgId -> mockOrgId.toString, V2PlayerCookieKeys.renderOptions -> opts)
    }

    "return body if all is ok and not secure" in new scope {
      call("""player.js?apiClient={}&options={"itemId":"*","sessionId":"*","secure":false}""") must returnResult(OK, "false")
    }
  }
}
