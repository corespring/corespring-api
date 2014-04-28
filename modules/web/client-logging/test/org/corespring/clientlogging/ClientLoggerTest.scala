package org.corespring.clientlogging

import org.corespring.test.BaseTest
import org.specs2.matcher.{Expectable, Matcher}
import play.api.libs.json.{Json, JsString, JsObject}
import play.api.mvc.{SimpleResult, AnyContentAsJson}
import play.api.test.Helpers._
import play.api.test._
import scala.concurrent.Future

class ClientLoggerTest extends BaseTest{


  case class returnResult(expectedStatus: Int) extends Matcher[(String, JsObject)] {
    def apply[S <: (String, JsObject)](s: Expectable[S]) = {
      val (logType : String, json : JsObject) = s.value
      val request = FakeRequest("", "", FakeHeaders(), AnyContentAsJson(json))
      val a = ClientLogger.submitLog(logType)
      val out = a(request)
      val callResult: Future[SimpleResult] = out
      val actualStatus = status(callResult)
      val body = contentAsString(callResult)
      val msg = s"$logType -> ${Json.stringify(json)} // $actualStatus <-> $expectedStatus: $body"
      result(actualStatus == expectedStatus, msg, msg, s)
    }
  }

  val msg = Json.obj("message" -> JsString("message"))
  val stackTrace = Json.obj("stacktrace" -> JsString("stacktrace"))

  "logging" should {
    "work" in {

      val out: Seq[(String, JsObject, Int)] = for {
        t <- Seq("fatal", "error", "warn", "debug", "info")
        json <- Seq(msg, msg ++ stackTrace)
      } yield (t, json, OK)

      forall(out) {
        t =>
          val (logType, json, status) = t
          (logType, json) must returnResult(status)
      }
    }
  }

  /*mainTest("bad-log-type", BAD_REQUEST)*/
}
