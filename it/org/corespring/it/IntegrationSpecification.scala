package org.corespring.it

import akka.util.Timeout
import org.specs2.execute.Results
import org.specs2.specification.Fragments
import play.api.test._
import scala.concurrent.duration._

abstract class IntegrationSpecification
  extends PlaySpecification
  with Results {

  sequential

  protected def logger: grizzled.slf4j.Logger

  override implicit def defaultAwaitTimeout: Timeout = 60.seconds

}

trait ServerSpec {

  implicit val app: FakeApplication = FakeApplication(

    additionalPlugins = Seq("se.radley.plugin.salat.SalatPlugin"),
    additionalConfiguration = Map(
      "logger" -> Map("play" -> "OFF", "application" -> "OFF"),
      "api.log-requests" -> false))

  implicit def port: Port = Helpers.testServerPort

  val server = TestServer(port, app)
}

