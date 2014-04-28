package org.corespring.test.matchers

import org.specs2.matcher.{Expectable, Matcher}
import scala.concurrent.Future
import play.api.mvc.SimpleResult
import play.api.test.Helpers._

trait RequestMatchers {

  /**
   * Usage: <code>myCall(request) must returnResult(OK,"ok")</code>
   * @param expectedStatus
   * @param body
   */
  case class returnResult(expectedStatus: Int, body: String) extends Matcher[Future[SimpleResult]] {
    def apply[S <: Future[SimpleResult]](s: Expectable[S]) = {
      val actualStatus = status(s.value)
      val actualBody = contentAsString(s.value)
      result(actualStatus == expectedStatus && actualBody == body,
        s"${actualStatus} matches $expectedStatus & $body",
        s"[$actualStatus:$actualBody] does not match [$expectedStatus:$body]",
        s)
    }
  }

  /**
   * Usage: <code>myCall(request) must haveCookies("x" -> "y", "a" -> "b")
   * @param cookies
   */
  case class haveCookies(cookies: (String, String)*) extends Matcher[Future[SimpleResult]] {
    def apply[S <: Future[SimpleResult]](s: Expectable[S]) = {
      val actualSession = session(s.value)

      val valueResults: Seq[(String, String)] = cookies.map {
        kv =>
          val valueResult = actualSession.get(kv._1).map {
            v =>
              if (v == kv._2) "equal" else "not equal"
          }.getOrElse("not found")
          (kv._1, valueResult)
      }

      val badResults = valueResults.filterNot(kv => kv._2 == "equal")
      val success = badResults.length == 0

      result(success,
        s"${cookies} matches ${valueResults.mkString(",")}",
        s"${cookies} != ${actualSession.data}",
        s)
    }
  }

}
