package org.corespring.v2player.integration.actionBuilders

import org.bson.types.ObjectId
import org.corespring.container.client.actions.PlayerJsRequest
import org.corespring.platform.core.controllers.auth.SecureSocialService
import org.corespring.platform.core.services.UserService
import org.corespring.test.PlaySingleton
import org.corespring.test.matchers.RequestMatchers
import org.corespring.v2.auth.OrgTransformer
import org.corespring.v2player.integration.actionBuilders.access.{ PlayerOptions, V2PlayerCookieKeys }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{ FakeHeaders, FakeRequest }

import scala.concurrent.Future
import scalaz.{ Failure, Success, Validation }

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

  case class scope(v: Validation[String, (ObjectId, PlayerOptions)]) extends Scope {

    def opts: Option[PlayerOptions] = v.toOption.map(_._2)

    val builder = new PlayerLauncherActions(mockSecureSocial, mock[UserService]) {

      def toOrgId(s: String): Option[ObjectId] = v.toOption.map(_._1)

      override def decrypt(request: RequestHeader, orgId: ObjectId, encrypted: String): Option[String] = None //if (decryptEnabled) Some(encrypted) else None

      override def transformer: OrgTransformer[(ObjectId, PlayerOptions)] = {
        val o = mock[OrgTransformer[(ObjectId, PlayerOptions)]]
        o.apply(any[RequestHeader]) returns v
      }
    }

    def handler(r: PlayerJsRequest[AnyContent]): Result = r.errors match {
      case Nil => Ok(s"${r.isSecure}")
      case _ => Ok(s"${r.errors.mkString(",")}")
    }

    def call(path: String): Future[SimpleResult] = builder.playerJs(handler)(FakeRequest("", path, FakeHeaders(), AnyContentAsEmpty))
  }

  "PlayerLauncherActionBuilder" should {

    "return failures from the transformer" in new scope(Failure("Bad")) {
      call("player.js") must returnResult(OK, "Bad")

    }

    "return body if all is ok and none secure" in new scope(
      Success(
        ObjectId.get,
        PlayerOptions(itemId = ObjectId.get.toString, secure = false))) {
      call("player.js") must returnResult(OK, "false")
    }

    "return body if all is ok and secure" in new scope(
      Success(
        mockOrgId,
        PlayerOptions(itemId = ObjectId.get.toString, secure = true))) {
      call("player.js") must returnResult(OK, "true")
      call("player.js") must haveCookies(V2PlayerCookieKeys.orgId -> mockOrgId.toString, V2PlayerCookieKeys.renderOptions -> Json.stringify(Json.toJson(opts.get)))
    }
  }
}
