package tests

import play.api.Play
import play.api.test.FakeApplication
import scala.Some
import scala.Some

/**
 * Utility to ensure only one instance of FakeApplication is started for tests
 */
object PlaySingleton {
  def start() {
    Play.maybeApplication match {
      case Some(fakeApp) =>
      case None => Play.start(FakeApplication())
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