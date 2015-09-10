package org.corespring.test

import play.api.Play
import play.api.test.FakeApplication
import scala.Some
import scala.Some
import java.io.File

/**
 * Utility to ensure only one instance of FakeApplication is started for tests
 */
object PlaySingleton {
  @deprecated("do you really need a play app running? if so use FakeApplication", "core-refactor")
  def start(): Unit = {
    Play.maybeApplication match {
      case Some(fakeApp) => Unit
      case None => {
        println(s" ----------------------------> Starting app ${new File(".").getAbsolutePath}")
        Play.start(
          FakeApplication(
            additionalPlugins = Seq("se.radley.plugin.salat.SalatPlugin")))
        Unit
      }
    }
  }

  def stop(): Unit = {
    Play.maybeApplication match {
      case Some(fakeApp) => {
        Play.stop()
        Unit
      }
      case None => Unit
    }
  }
}