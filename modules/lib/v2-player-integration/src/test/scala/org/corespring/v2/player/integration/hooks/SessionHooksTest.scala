package org.corespring.v2.player.integration.hooks

import org.corespring.container.client.hooks.FullSession
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.item.resource.StoredFile
import org.corespring.platform.core.models.item.{Item, PlayerDefinition}
import org.corespring.test.matchers.RequestMatchers
import org.corespring.v2.auth.SessionAuth
import org.corespring.v2.auth.SessionAuth.Session
import org.corespring.v2.auth.models.AuthMode.AuthMode
import org.corespring.v2.auth.models.{PlayerAccessSettings, OrgAndOpts}
import org.corespring.v2.errors.Errors.{cantLoadSession, generalError, invalidToken}
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.hooks.SessionHooks
import org.specs2.matcher.{Expectable, Matcher}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{Json, JsValue}
import play.api.mvc.RequestHeader

import scalaz.{Failure, Success, Validation}

class SessionHooksTest extends Specification with Mockito with RequestMatchers {

  import scala.language.higherKinds

  val defaultFailure = generalError("Default failure")

  trait MockResults {
    def getOrgAndOptionsResult: Validation[V2Error, OrgAndOpts]

    def authSaveSessionResult: Validation[V2Error, (String, Session) => Option[Session]]

    def authLoadForReadResult: Validation[V2Error, (Session, PlayerDefinition)]
  }

  abstract class SessionContext extends Scope with MockResults {

    lazy val hooks = new SessionHooks {

      override def auth: SessionAuth[OrgAndOpts, PlayerDefinition] = {
        val m = mock[SessionAuth[OrgAndOpts, PlayerDefinition]]
        m.saveSession(any[OrgAndOpts]) returns authSaveSessionResult
        m.loadForRead(anyString)(any[OrgAndOpts]) returns authLoadForReadResult
        m
      }

      override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = getOrgAndOptionsResult

      override def itemService: org.corespring.platform.core.services.item.ItemService = ???

      override def transformItem: Item => JsValue = ???
    }
  }

  case class returnStatusMessage[D](expectedStatus: Int, body: String) extends Matcher[Either[(Int, String), D]] {
    def apply[S <: Either[(Int, String), D]](s: Expectable[S]) = {

      println(s" --> ${s.value}")
      def callResult(success: Boolean) = result(success, s"${s.value} matches $expectedStatus & $body", s"${s.value} doesn't match $expectedStatus & $body", s)
      s.value match {
        case Left((code, msg)) => callResult(code == expectedStatus && msg == body)
        case Right(_) => callResult(false)
      }
    }
  }

  case class returnFullSession[D](expectedStatus: Int, body: String) extends Matcher[Either[(Int, String), D]] {
    def apply[S <: Either[(Int, String), D]](s: Expectable[S]) = {

      println(s" --> ${s.value}")
      def callResult(success: Boolean) = result(success, s"${s.value} matches $expectedStatus & $body", s"${s.value} doesn't match $expectedStatus & $body", s)
      s.value match {
        case Left((code, msg)) => callResult(code == expectedStatus && msg == body)
        case Right(_) => callResult(false)
      }
    }
  }

  "loadItemAndSession" should {

    "returns error if can't resolve identity" in new SessionContext {
      override def getOrgAndOptionsResult = Failure(invalidToken(mock[RequestHeader]))
      override def authSaveSessionResult = Success((s: String, session: Session) => Some(mock[Session]))
      override def authLoadForReadResult = Success((mock[Session], mock[PlayerDefinition]))

      hooks.loadItemAndSession("test_session_id")(mock[RequestHeader]) must
        returnStatusMessage(getOrgAndOptionsResult.toEither.left.get.statusCode,
          getOrgAndOptionsResult.toEither.left.get.message)
    }

    "returns error if can't authenticate" in new SessionContext {
      override def getOrgAndOptionsResult = Success(mock[OrgAndOpts])
      override def authSaveSessionResult = Success((s: String, session: Session) => Some(mock[Session]))
      override def authLoadForReadResult = Failure(cantLoadSession("sessionId"))

      hooks.loadItemAndSession("test_session_id")(mock[RequestHeader]) must
        returnStatusMessage(authLoadForReadResult.toEither.left.get.statusCode,
          authLoadForReadResult.toEither.left.get.message)
    }

    "returns full session if when all went good" in new SessionContext {
      override def getOrgAndOptionsResult = Success(OrgAndOpts(mock[Organization],
          PlayerAccessSettings(itemId="itemId"),mock[AuthMode]))

      override def authSaveSessionResult = Success((s: String, session: Session) => Some(mock[Session]))
      override def authLoadForReadResult = {
        val session = mock[Session]
        val playerDef = new PlayerDefinition(
          Seq(StoredFile("test.js", "text/javascript", false, "key")),
          "",
          Json.obj(),
          "", Some("function(){}")
        )

        Success((session,playerDef))
      }

      val res = hooks.loadItemAndSession("test_session_id")(mock[RequestHeader])
      res.isRight === true
      res match {
        case Right(FullSession(js,isSecure)) => success
        case _ => failure("Error")
      }
    }
  }

}
