package org.corespring.it

import play.api.test.{Port, Helpers, TestServer, FakeApplication}

trait ServerSpec {

  implicit val app: FakeApplication = FakeApplication(

    additionalPlugins = Seq("se.radley.plugin.salat.SalatPlugin"),
    additionalConfiguration = Map("logger" -> Map( "play" -> "OFF", "application" -> "OFF"))
  )

  implicit def port: Port = Helpers.testServerPort

  val server = TestServer(port, app)
}

