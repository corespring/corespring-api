package tests.functional

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

import org.fluentlenium.core.filter.FilterConstructor._

class IntegrationSpec extends Specification {

  "Application" should {

    "work from within a browser" in {
      running(TestServer(3333), HTMLUNIT) { browser =>
        browser.goTo("http://localhost:3333/web")
        browser.title() must equalTo("Play 2.0 sample application — Computer database")
      }
    }

  }

}