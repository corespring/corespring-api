package org.corespring.clientlogging

import org.corespring.test.BaseTest
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import play.api.mvc.AnyContentAsJson
import play.api.libs.json.{JsString, JsObject}
import org.specs2.specification.{Fragments, Fragment}

class ClientLoggerTest extends BaseTest{
  def succeedWithMessage(logType:String) = {
    val json = JsObject(Seq("message" -> JsString("blergl mergl")))
    val request = FakeRequest(POST, s"/logger/$logType", FakeHeaders(), AnyContentAsJson(json))
    val result = route(request).get
    status(result) must beEqualTo(OK)
  }
  def succeedWithMessageAndStackTrace(logType:String) = {
    val json = JsObject(Seq("message" -> JsString("blergl mergl"), "stacktrace" -> JsString("more mergl")))
    val request = FakeRequest(POST, s"/logger/$logType", FakeHeaders(), AnyContentAsJson(json))
    val result = route(request).get
    status(result) must beEqualTo(OK)
  }
  def failWithoutMessage(logType:String) = {
    val request = FakeRequest(POST, s"/logger/$logType", FakeHeaders(), AnyContentAsJson(JsObject(Seq())))
    val result = route(request).get
    status(result) must beEqualTo(BAD_REQUEST)
  }
  def mainTest(logType:String):Fragments = {
    s"submitting a log entry type $logType" should {
      "succeed with message" in {
        succeedWithMessage(logType)
      }
      "succeed with message and stacktrace" in  {
        succeedWithMessageAndStackTrace(logType)
      }
      "fail without message" in {
        failWithoutMessage(logType)
      }
    }
  }
  mainTest("fatal")
  mainTest("error")
  mainTest("warn")
  mainTest("debug")
  mainTest("info")
  "submitting a log entry of any other type" should {
    "result in error" in {
      val json = JsObject(Seq("message" -> JsString("blergl mergl")))
      val request = FakeRequest(POST, "/logger/meh", FakeHeaders(), AnyContentAsJson(json))
      val result = route(request).get
      status(result) must beEqualTo(BAD_REQUEST)
    }
  }
}
