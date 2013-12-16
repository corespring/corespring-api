package tests.filters

import org.specs2.mutable.Specification
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import scala.concurrent.Future
import scala.concurrent.duration._

abstract class FilterTest(filter: EssentialFilter) extends Specification {

  val timeout = Duration(1000, SECONDS)

  val okAction = Action {
    request => {
      Ok("")
    }
  }

  def givenRequestHeader(requestHeader: (String, String)) = givenRequestHeaders(requestHeader)

  def givenNoRequestHeaders = givenRequestHeaders()

  def givenRequestHeaders(requestHeaders: (String, String)*) = {
    val request = FakeRequest("", "/", FakeHeaders(), "").withHeaders(requestHeaders : _*)
    val result = filter(okAction)(request)
    val future: Future[SimpleResult] = result.run
    future
  }

  protected class ResultWithHeaderChecking(futureResponse: Future[SimpleResult]) {

    import akka.util._
    implicit val timeout : Timeout = Timeout(1000)

    private lazy val  response = play.api.test.Helpers.await(futureResponse)

    def shouldHaveResponseHeader(responseHeader: (String, String)) = shouldHaveResponseHeaders(responseHeader)

    def shouldHaveResponseHeaders(responseHeaders: (String, String)*) = {
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

  implicit def resultWithHeaderChecking(response: Future[SimpleResult]) = new ResultWithHeaderChecking(response)

}
