package org.corespring.it

import java.io.File

import play.api.Play
import play.api.test.FakeApplication

object ITContext {

  def setup = {
    println(s"[ITContext] setup")
    //1. a running play app
    //2. the services object
    PlaySingleton.start()
  }

  def cleanup = {
    PlaySingleton.stop()
  }
}

private object PlaySingleton {
  def start() = Play.maybeApplication.map(_ => Unit).getOrElse {
    println(s"[PlaySingleton] ----------------------------> Starting app ${new File(".").getAbsolutePath}")

    val config = Map("logger" -> Map("play" -> "OFF", "application" -> "OFF"), "api.log-requests" -> false)

    val app: FakeApplication = FakeApplication(
      additionalPlugins = Seq("se.radley.plugin.salat.SalatPlugin"),
      additionalConfiguration = config)

    Play.start(app)
  }

  def stop() = Play.maybeApplication.foreach(_ => Play.stop())

}
