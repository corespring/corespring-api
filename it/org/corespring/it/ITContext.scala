package org.corespring.it

import java.io.File

import play.api.Play
import play.api.test.FakeApplication

object ITContext {

  def setup = {
    println(s"[ITContext] setup")
    PlaySingleton.start()
  }

  def cleanup = {
    println(s"[ITContext] cleanup")
    PlaySingleton.stop()
  }
}

private object PlaySingleton {

  def log(s: String) = {
    println(s"[PlaySingleton] -> $s")
  }

  def start() = Play.maybeApplication.map(_ => Unit).getOrElse {
    log(s"starting app ${new File(".").getAbsolutePath}")

    val config = Map("logger" -> Map("play" -> "OFF", "application" -> "OFF"), "api.log-requests" -> false)

    val app: FakeApplication = FakeApplication(
      additionalPlugins = Seq("se.radley.plugin.salat.SalatPlugin"),
      additionalConfiguration = config)

    Play.start(app)
  }

  def stop() = {
    log(s"stopping app ${new File(".").getAbsolutePath}")
    Play.maybeApplication.foreach(_ => Play.stop())
  }

}
