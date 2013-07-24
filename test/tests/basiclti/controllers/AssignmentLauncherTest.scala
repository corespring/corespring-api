package tests.basiclti.controllers


import play.api.mvc._
import play.api.test.{WithApplication, FakeHeaders, FakeRequest}
import play.api.test.Helpers._

import org.specs2.mutable._

class AssignmentLauncherTest extends Specification /*extends BaseTest*/ {

  case class FakeRequestWithHost[A](
                                     method: String,
                                     uri: String,
                                     headers: FakeHeaders,
                                     body: A,
                                     remoteAddress: String = "127.0.0.1",
                                     hostOverride: String = "http://localhost") extends play.api.mvc.Request[A]{
    def id: Long = 1

    def tags: Map[String, String] = Map()

    def path: String = "path"

    def version: String = "version"

    def queryString: Map[String, Seq[String]] = Map()
  }


  val routes = basiclti.controllers.routes.AssignmentLauncher

  "upgrade" should {

    "ping" in new WithApplication {
      val request =  FakeRequest("GET", "/", FakeHeaders(), AnyContentAsEmpty)
      val result = route(request).get
      status(result) === OK
    }

    "ping 2" in new WithApplication {
      val request =  FakeRequest(routes.launch().method, routes.launch().url, FakeHeaders(), AnyContentAsEmpty)
      val result = route(request).get
      status(result) === BAD_REQUEST
    }

    "skipped" in new WithApplication{
      //TODO 2.1.1 - re-add the tests below
      pending("see https://gist.github.com/415f1322df9c2df10487")
      true === true
    }
  }

}
