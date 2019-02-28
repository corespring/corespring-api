package org.corespring.container.client.controllers.resources

import org.corespring.container.client.hooks._
import org.corespring.container.components.outcome.ScoreProcessor
import org.corespring.container.components.processing.PlayerItemPreProcessor
import org.corespring.container.components.response.OutcomeProcessor
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{ FakeHeaders, FakeRequest }

import scala.concurrent.{ ExecutionContext, Future }

class SessionTest extends Specification with Mockito {

  trait scope extends Scope {
    def isSecure = true
    def saveSession(isComplete: Boolean) = SaveSession(
      Json.obj(),
      isSecure,
      isComplete,
      (a, b) => Some(Json.obj()))

    def outcome(itemSession: JsValue = Json.obj(), isComplete: Boolean) = SessionOutcome(
      Json.obj(),
      itemSession,
      isSecure,
      isComplete)

    def fullSessionAndItem = Right(FullSession(
      Json.obj(
        "item" -> Json.obj(),
        "session" -> Json.obj()),
      isSecure))

    lazy val outcomeProcessor: OutcomeProcessor = {
      val m = mock[OutcomeProcessor]
      m.createOutcome(any[JsValue], any[JsValue], any[JsValue]) returns Json.obj()
      m
    }

    lazy val scoreProcessor: ScoreProcessor = {
      val m = mock[ScoreProcessor]
      m.score(any[JsValue], any[JsValue], any[JsValue]) returns Json.obj()
      m
    }

    lazy val req = FakeRequest()

    def itemPreProcessor: PlayerItemPreProcessor = {
      val m = mock[PlayerItemPreProcessor]
      m.preProcessItemForPlayer(any[JsValue]) answers (json => json.asInstanceOf[JsValue])
    }

    lazy val hooks: SessionHooks = {
      val m = mock[SessionHooks]
      m.loadItemAndSession(any[String])(any[RequestHeader]) returns fullSessionAndItem

      m.getScore(any[String])(any[RequestHeader]) returns Right(outcome(isComplete = true))

      m.save(any[String])(any[RequestHeader]) returns Future.successful {
        Right(saveSession(isComplete = true))
      }

      m.loadOutcome(any[String])(any[RequestHeader]) returns {
        Right(SessionOutcome(Json.obj(), Json.obj(), true, true))
      }
      m
    }

    lazy val sessionContext = SessionExecutionContext(ExecutionContext.global, ExecutionContext.global)

    val session = new Session(outcomeProcessor, itemPreProcessor, scoreProcessor, hooks, sessionContext)
  }

  "secure mode" should {

    "when saving" should {

      "allow save when session is not complete" in new scope {

        hooks.save(any[String])(any[RequestHeader]) returns {
          Future.successful(Right(saveSession(false)))
        }

        val result = session.saveSession("id")(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        status(result) === OK
      }

      "not allow save when session is complete" in new scope {
        hooks.save(any[String])(any[RequestHeader]) returns {
          Future.successful(Right(saveSession(true)))
        }

        val result = session.saveSession("id")(req)
        status(result) === BAD_REQUEST
        (contentAsJson(result) \ "error").as[String] === Session.Errors.cantSaveWhenComplete
      }
    }

    "when loading outcome" should {

      "not allow load outcome when session is complete, but there are no answers" in new scope {
        hooks.loadOutcome(any[String])(any[RequestHeader]) returns {
          Right(outcome(isComplete = true))
        }
        val result = session.loadOutcome("id")(req)
        status(result) === BAD_REQUEST
      }

      "allow load outcome when session is complete" in new scope {

        hooks.loadOutcome(any[String])(any[RequestHeader]) returns {
          Right(
            outcome(itemSession = Json.obj("components" -> Json.obj()), isComplete = true))
        }

        val result = session.loadOutcome("id")(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        status(result) === OK
      }

      "not allow load outcome when session is not complete" in new scope {
        hooks.loadOutcome(any[String])(any[RequestHeader]) returns {
          Right(
            outcome(isComplete = false))
        }
        val result = session.loadOutcome("id")(req)
        status(result) === BAD_REQUEST
      }
    }

    "when scoring" should {

      "not allow load getScore when session is complete, but there are no answers" in new scope {
        hooks.getScore(any[String])(any[RequestHeader]) returns {
          Right(outcome(isComplete = true))
        }
        val result = session.getScore("id")(req)
        status(result) === BAD_REQUEST
      }

      "allow load getScore when session is complete" in new scope {
        hooks.getScore(any[String])(any[RequestHeader]) returns {
          Right(outcome(itemSession = Json.obj("components" -> Json.obj()), isComplete = true))
        }
        val result = session.getScore("id")(req)
        status(result) === OK
      }

      "not allow getScore when session is not complete" in new scope {
        hooks.getScore(any[String])(any[RequestHeader]) returns {
          Right(outcome(isComplete = false))
        }
        val result = session.getScore("id")(req)
        status(result) === BAD_REQUEST
      }
    }

    "when loading session and item" should {

      trait LoadItemAndSessionResponding extends scope {
        hooks.loadItemAndSession(any[String])(any[RequestHeader]) returns {
          Left(BAD_REQUEST -> "")
        }
      }

      "return when session hooks returned failure" in new LoadItemAndSessionResponding {
        val result = session.loadItemAndSession("id")(req)
        status(result) === BAD_REQUEST
      }

      "return session when session hooks succeeded" in new LoadItemAndSessionResponding {
        hooks.loadItemAndSession(any[String])(any[RequestHeader]) returns fullSessionAndItem
        val result = session.loadItemAndSession("id")(req)
        val asJson = contentAsJson(result)
        (asJson \ "item") === Json.obj()
        (asJson \ "session") === Json.obj()
        status(result) === OK
      }
    }

    "not allow to reset session" in new scope {
      hooks.save(any[String])(any[RequestHeader]) returns Future.successful(Right(saveSession(true)))
      val result = session.resetSession("id")(req)
      status(result) === BAD_REQUEST
    }

    "not allow to reopen session" in new scope {
      hooks.save(any[String])(any[RequestHeader]) returns Future.successful(Right(saveSession(true)))
      val result = session.reopenSession("id")(req)
      status(result) === BAD_REQUEST
    }

  }

  "not secure mode" should {

    trait insecureSaveSession extends scope {

      def unsaveSession() = SaveSession(
        Json.obj("attempts" -> 123, "components" -> Json.arr(1, 2, 3)),
        isSecure = false,
        isComplete = true,
        (a, b) => Some(b))

      hooks.save(any[String])(any[RequestHeader]) returns {
        Future.successful(Right(unsaveSession()))
      }
    }

    "allow to reset session" in new insecureSaveSession {
      val result = session.resetSession("id")(req)
      status(result) === OK
      val resettedSession = contentAsJson(result)
      (resettedSession \ "isComplete").as[Boolean] === false
      (resettedSession \ "attempts").as[Int] === 0
      (resettedSession \ "components") === Json.obj()
    }

    "allow to reopen session" in new insecureSaveSession {
      val result = session.reopenSession("id")(req)
      status(result) === OK
      val reopenedSession = contentAsJson(result)
      (reopenedSession \ "isComplete").as[Boolean] === false
      (reopenedSession \ "attempts").as[Int] === 0
    }
  }
}
