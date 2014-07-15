package org.corespring.reporting.services

import org.specs2.mutable.Specification
import play.api.Play
import java.io.File
import play.api.test.FakeApplication


object PlaySingleton {
  def start() : Unit = {
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

  def stop() : Unit = {
    Play.maybeApplication match {
      case Some(fakeApp) => {
        Play.stop()
        Unit
      }
      case None => Unit
    }
  }
}

class Hook extends Specification {

  PlaySingleton.start()

  "something" should {


    "print report headers" in {
      println(ReportsService.buildStandardsGroupReport())
      true === true
    }

  }

}
