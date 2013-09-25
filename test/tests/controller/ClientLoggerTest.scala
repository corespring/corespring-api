package tests.controller

import org.corespring.test.BaseTest
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import play.api.mvc.AnyContentAsJson
import play.api.libs.json.{JsString, JsObject}

class ClientLoggerTest extends BaseTest{
  "submitting a log entry type fatal" should {
    "succeed with message" in {
      val json = JsObject(Seq("message" -> JsString("blergl mergl")))
      val request = FakeRequest(POST, "/logger/fatal", FakeHeaders(), AnyContentAsJson(json))
      val result = route(request).get
      status(result) must beEqualTo(OK)
    }
    "succeed with message and stacktrace" in {
      val json = JsObject(Seq("message" -> JsString("blergl mergl"), "stacktrace" -> JsString("more mergl")))
      val request = FakeRequest(POST, "/logger/fatal", FakeHeaders(), AnyContentAsJson(json))
      val result = route(request).get
      status(result) must beEqualTo(OK)
    }
    "fail without message" in {
      val request = FakeRequest(POST, "/logger/fatal", FakeHeaders(), AnyContentAsJson(JsObject(Seq())))
      val result = route(request).get
      status(result) must beEqualTo(BAD_REQUEST)
    }
  }
  "submitting a log entry type error" should {
    "succeed with message" in {
      pending
    }
    "succeed with message and stacktrace" in {
      pending
    }
    "fail without message" in {
      pending
    }
  }
  "submitting a log entry type warn" should {
    "succeed with message" in {
      pending
    }
    "fail without message" in {
      pending
    }
  }
  "submitting a log entry type info" should {
    "succeed with message" in {
      pending
    }
    "fail without message" in {
      pending
    }
  }
  "submitting a log entry type debug" should {
    "succeed with message" in {
      pending
    }
    "fail without message" in {
      pending
    }
  }
  //FakeRequest(POST,"/logger/fatal")
}
