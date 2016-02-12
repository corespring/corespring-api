package org.corespring.v2.sessiondb.webservice

import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.sessions.SessionServiceClient
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.Json
import se.radley.plugin.salat.Binders.ObjectId

import scala.concurrent._
import scalaz._

class RemoteSessionServiceTest extends Specification with Mockito {

  trait RemoteSessionServiceScope extends Scope {

    val sessionServiceClient = mock[SessionServiceClient]
    val remoteSessionService = new RemoteSessionService(sessionServiceClient)

    val itemId = new VersionedId[ObjectId](new ObjectId(), Some(0))
    val itemCount = 12

    val sessionId = new ObjectId()
    val sessionJson = Json.obj("hey" -> "this", "is" -> "some", "session" -> "json")

    val errorMessage = "Oops. There was an error on the server."
  }

  "sessionCount" should {

    trait SessionCountSuccess extends RemoteSessionServiceScope {
      sessionServiceClient.sessionCount(itemId.toString).returns(Future.successful(Success(itemCount)))
      val result = remoteSessionService.sessionCount(itemId)
    }

    "return result from client" in new SessionCountSuccess {
      result must be equalTo(itemCount)
    }

    "client returns error" should {

      trait SessionCountFailure extends RemoteSessionServiceScope {
        sessionServiceClient.sessionCount(itemId.toString).returns(Future.successful(Failure(new Error(errorMessage))))
      }

      "throw an exception with the provided error message" in new SessionCountFailure {
        { remoteSessionService.sessionCount(itemId) } must throwA(new Exception(errorMessage))
      }

    }

  }

  "load" should {

    trait SessionLoadSuccess extends RemoteSessionServiceScope {
      sessionServiceClient.get(sessionId).returns(Future.successful(Success(Some(sessionJson))))
    }

    "return session from client" in new SessionLoadSuccess {
      remoteSessionService.load(sessionId.toString) must be equalTo(Some(sessionJson))
    }

    "session not found by client" should {

      trait SessionNotFound extends RemoteSessionServiceScope {
        sessionServiceClient.get(sessionId).returns(Future.successful(Success(None)))
      }

      "return None" in new SessionNotFound {
        remoteSessionService.load(sessionId.toString) must beEmpty
      }

    }

    "client returns an Error" should {

      trait SessionLoadError extends RemoteSessionServiceScope {
        sessionServiceClient.get(sessionId).returns(Future.successful(Failure(new Error(errorMessage))))
      }

      "return None" in new SessionLoadError {
        remoteSessionService.load(sessionId.toString) must beEmpty
      }

    }

  }

  "save" should {

    trait SessionSaveSuccess extends RemoteSessionServiceScope {
      sessionServiceClient.update(sessionId, sessionJson).returns(Future.successful(Success(sessionJson)))
    }

    "return session from client" in new SessionSaveSuccess {
      remoteSessionService.save(sessionId.toString, sessionJson) must be equalTo(Some(sessionJson))
    }

    "client returns an Error" should {

      trait SessionSaveError extends RemoteSessionServiceScope {
        sessionServiceClient.update(sessionId, sessionJson).returns(Future.successful(Failure(new Error(errorMessage))))
      }

      "return None" in new SessionSaveError {
        remoteSessionService.save(sessionId.toString, sessionJson) must beEmpty
      }

    }

  }

  "create" should {

    trait SessionCreateSuccess extends RemoteSessionServiceScope {
      sessionServiceClient.create(sessionJson).returns(
        Future.successful(Success(sessionJson ++ Json.obj("id" -> sessionId.toString))))
    }

    "returns 'id' field from JSON response" in new SessionCreateSuccess {
      remoteSessionService.create(sessionJson) must be equalTo(Some(sessionId))
    }

    "client returns an Error" should {

      trait SessionCreateFailure extends RemoteSessionServiceScope {
        sessionServiceClient.create(sessionJson).returns(Future.successful(Failure(new Error(errorMessage))))
      }

      "return None" in new SessionCreateFailure {
        remoteSessionService.create(sessionJson) must beEmpty
      }

    }

  }

  "orgCount" should {

    val orgId = new ObjectId().toString
    val month = new DateTime()
    val results = {
      val r = scala.util.Random
      1.to(31).map(day => new DateTime().withMonthOfYear(1).withDayOfMonth(day) -> r.nextInt(100).toLong).toMap
    }

    trait OrgCountSuccess extends RemoteSessionServiceScope {
      sessionServiceClient.sessionOrgCount(orgId, month).returns(Future.successful(Success(results)))
    }

    "return count from client" in new OrgCountSuccess {
      remoteSessionService.orgCount(new ObjectId(orgId), month) must be equalTo(Some(results))
    }

    "client returns an Error" should {

      trait OrgCountFailure extends RemoteSessionServiceScope {
        sessionServiceClient.sessionOrgCount(orgId, month).returns(Future.successful(Failure(new Error(errorMessage))))
      }

      "return None" in new OrgCountFailure {
        remoteSessionService.orgCount(new ObjectId(orgId), month) must beEmpty
      }

    }

  }

}
