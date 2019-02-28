package org.corespring.container.client.controllers

import org.corespring.container.client.hooks.DataQueryHooks
import org.corespring.test.TestContext
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.JsArray
import play.api.mvc.RequestHeader
import play.api.test.Helpers._
import play.api.test.{ FakeApplication, FakeRequest }

import scala.concurrent.{ ExecutionContext, Future }

class DataQueryTest extends Specification with Mockito {

  object mockGlobal extends play.api.GlobalSettings

  import ExecutionContext.Implicits.global

  "data query" should {

    val hooks: DataQueryHooks = {
      val m = mock[DataQueryHooks]
      m.list(anyString, any[Option[String]])(any[RequestHeader]) returns Future(Right(JsArray(Seq.empty)))
      m
    }

    val dq = new DataQuery(hooks, TestContext.containerContext)

    "return an error for an invalid topic" in running(FakeApplication(withGlobal = Some(mockGlobal))) {
      val result = dq.list("bad-topic")(FakeRequest("", ""))
      status(result) === BAD_REQUEST
    }

    "return OK" in running(FakeApplication(withGlobal = Some(mockGlobal))) {
      val result = dq.list("itemTypes")(FakeRequest("", ""))
      status(result) === OK
    }
  }
}
