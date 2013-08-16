package tests.filters

import play.api.test.FakeRequest
import scala.concurrent.{Await, Future}
import play.api.mvc._
import scala.concurrent.duration._
import play.api.mvc.Results._
import play.api.test.FakeHeaders
import play.api.mvc.SimpleResult
import scala.Tuple2
import org.specs2.mutable.Specification

class FilterTest(filter: EssentialFilter) extends Specification {

  val timeout = Duration(1000, SECONDS)

  val okAction = Action {
    request => {
      Ok("")
    }
  }

  def givenRequestHeader(requestHeader: Tuple2[String, String]) = givenRequestHeaders(requestHeader)

  def givenNoRequestHeaders = givenRequestHeaders()

  def givenRequestHeaders(requestHeaders: Tuple2[String, String]*) = {
    val request = FakeRequest("", "/", FakeHeaders(), "").withHeaders(requestHeaders : _*)
    val result = filter(okAction)(request)
    val future: Future[Result] = result.run
    Await.result(future, timeout).asInstanceOf[SimpleResult[String]]
  }

  protected class ResultWithHeaderChecking(response: PlainResult) {

    def shouldHaveResponseHeader(responseHeader: Tuple2[String, String]) = shouldHaveResponseHeaders(responseHeader)

    def shouldHaveResponseHeaders(responseHeaders: Tuple2[String, String]*) = {
      responseHeaders.map{ responseHeader =>
        response.header.headers.get(responseHeader._1) match {
          case Some(string) => string == responseHeader._2
          case None => "" == responseHeader._2
        }
      }.contains(false) === false
    }

    def shouldNotHaveResponseHeader(responseHeader: String) = {
      response.header.headers.get(responseHeader) === None
    }
  }

  implicit def resultWithHeaderChecking(response: PlainResult) = new ResultWithHeaderChecking(response)

}
