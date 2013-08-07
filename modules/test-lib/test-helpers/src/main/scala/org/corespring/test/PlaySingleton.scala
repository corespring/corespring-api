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
  def start() {
    Play.maybeApplication match {
      case Some(fakeApp) =>
      case None => {
        println(s" ----------------------------> Starting app ${new File(".").getAbsolutePath}")
        Play.start(
          FakeApplication(

            additionalPlugins =  Seq("se.radley.plugin.salat.SalatPlugin")
          )
        )
      }
    }
  }

  def stop() {
    Play.maybeApplication match {
      case Some(fakeApp) => {
        Play.stop()
      }
      case None =>
    }
  }
}