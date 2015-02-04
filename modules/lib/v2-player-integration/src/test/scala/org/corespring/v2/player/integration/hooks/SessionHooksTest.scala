package org.corespring.v2.player.integration.hooks

import akka.util.internal.Timeout
import org.corespring.container.client.hooks.{SaveSession, SessionOutcome, FullSession}
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
import play.api.GlobalSettings
import play.api.libs.json.{Json, JsValue}
import play.api.mvc.RequestHeader
import play.api.test.FakeApplication
import play.api.test.Helpers._

import scalaz.{Failure, Success, Validation}

class SessionHooksTest extends Specification with Mockito with RequestMatchers {

  import scala.language.higherKinds

  val defaultFailure = generalError("Default failure")

  abstract class SessionContext extends Scope {

    def getOrgAndOptionsResult: Validation[V2Error, OrgAndOpts]
    def authSaveSessionResult: Validation[V2Error, (String, Session) => Option[Session]]
    def authLoadForReadResult: Validation[V2Error, (Session, PlayerDefinition)]

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

  class IdentificationFailureContext extends SessionContext{
    override def getOrgAndOptionsResult = Failure(invalidToken(mock[RequestHeader]))
    override def authSaveSessionResult = Success((s: String, session: Session) => Some(mock[Session]))
    override def authLoadForReadResult = Success((mock[Session], mock[PlayerDefinition]))
  }

  class AuthenticationFailureContext extends SessionContext{
    override def getOrgAndOptionsResult = Success(mock[OrgAndOpts])
    override def authSaveSessionResult = Success((s: String, session: Session) => Some(mock[Session]))
    override def authLoadForReadResult = Failure(cantLoadSession("sessionId"))
  }

  class Successful_Identification_And_Authentication_Context extends SessionContext {
    override def getOrgAndOptionsResult = Success(OrgAndOpts(mock[Organization],
      PlayerAccessSettings(itemId = "itemId"), mock[AuthMode]))

    override def authSaveSessionResult = Success((s: String, session: Session) => Some(mock[Session]))

    override def authLoadForReadResult = {
      val session = Json.obj("isComplete" -> true)
      val playerDef = new PlayerDefinition(
        Seq(StoredFile("test.js", "text/javascript", false, "key")),
        "",
        Json.obj(),
        "", Some("function(){}")
      )

      Success((session, playerDef))
    }
  }

  "loadItemAndSession method" should {

    "return error if can't resolve identity" in new IdentificationFailureContext {

      hooks.loadItemAndSession("test_session_id")(mock[RequestHeader]) must
        returnStatusMessage(getOrgAndOptionsResult.toEither.left.get.statusCode,
          getOrgAndOptionsResult.toEither.left.get.message)
    }

    "return error if can't authenticate" in new AuthenticationFailureContext {

      hooks.loadItemAndSession("test_session_id")(mock[RequestHeader]) must
        returnStatusMessage(authLoadForReadResult.toEither.left.get.statusCode,
          authLoadForReadResult.toEither.left.get.message)
    }

    "return full session if when all went good" in new Successful_Identification_And_Authentication_Context {

      val res = hooks.loadItemAndSession("test_session_id")(mock[RequestHeader]) match {
        case Right(FullSession(js,isSecure)) => success
        case _ => failure("Error")
      }
    }
  }

    "loadOutcome method" should {

      "return error if can't resolve identity" in new IdentificationFailureContext {

        hooks.loadOutcome("test_session_id")(mock[RequestHeader]) must
          returnStatusMessage(getOrgAndOptionsResult.toEither.left.get.statusCode,
            getOrgAndOptionsResult.toEither.left.get.message)
      }

      "return error if can't authenticate" in new AuthenticationFailureContext {

        hooks.loadOutcome("test_session_id")(mock[RequestHeader]) must
          returnStatusMessage(authLoadForReadResult.toEither.left.get.statusCode,
            authLoadForReadResult.toEither.left.get.message)
      }

      "return SessionOutcome if all went good" in new Successful_Identification_And_Authentication_Context {

        hooks.loadOutcome("test_session_id")(mock[RequestHeader]) match {
          case Right(SessionOutcome(item, session, secure, isComplete)) => success
          case _ => failure("Error")
        }
      }
    }

    "getScore method" should {

      "return error if can't resolve identity" in new IdentificationFailureContext {

        hooks.getScore("test_session_id")(mock[RequestHeader]) must
          returnStatusMessage(getOrgAndOptionsResult.toEither.left.get.statusCode,
            getOrgAndOptionsResult.toEither.left.get.message)
      }

      "return error if can't authenticate" in new AuthenticationFailureContext {

        hooks.getScore("test_session_id")(mock[RequestHeader]) must
          returnStatusMessage(authLoadForReadResult.toEither.left.get.statusCode,
            authLoadForReadResult.toEither.left.get.message)
      }

      "return SessionOutcome if all went good" in new Successful_Identification_And_Authentication_Context {

        hooks.getScore("test_session_id")(mock[RequestHeader]) match {
          case Right(SessionOutcome(item, session, secure, isComplete)) => success
          case _ => failure("Error")
        }
      }
    }

  object mockGlobal extends GlobalSettings {
    sequential
      "save method" should {

        running( FakeApplication( withGlobal = Some(mockGlobal))) {
          "return error if can't resolve identity" in new IdentificationFailureContext {

            val futureResult = hooks.save("test_session_id")(mock[RequestHeader])

            futureResult must
              returnStatusMessage(getOrgAndOptionsResult.toEither.left.get.statusCode,
                getOrgAndOptionsResult.toEither.left.get.message).await
          }

          "return error if can't authenticate" in new AuthenticationFailureContext {

            hooks.save("test_session_id")(mock[RequestHeader]) must
              returnStatusMessage(authLoadForReadResult.toEither.left.get.statusCode,
                authLoadForReadResult.toEither.left.get.message).await
          }

          "return SessionOutcome if all went good" in new Successful_Identification_And_Authentication_Context {

            hooks.save("test_session_id")(mock[RequestHeader]) must new Matcher[Either[(Int, String), SaveSession]] {
              def apply[S <: Either[(Int, String), SaveSession]](s: Expectable[S]) = {

                s.value match {
                  case Right(SaveSession(item, session, secure, isComplete)) =>
                    result(true,"result matches the SaveSession","result doesnt match SaveSession",s)
                  case Left(_) => result(false," "," result dosent match SaveSession ",s)
                }
              }
            }.await
          }
        }
      }
    }

}
