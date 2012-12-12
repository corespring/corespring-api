package tests.basiclti.controllers

import org.specs2.mutable.Specification
import org.specs2.execute.Pending
import tests.PlaySingleton

class LaunchConfigTest extends Specification {

  PlaySingleton.start()

  "launch config" should {

    "return a config" in {
      Pending("coming soon..")
    }

    "update a config" in {
      Pending("coming soon...")
    }

    "not allow an update if the user org doesn't match the db org" in {
      Pending("coming soon...")
    }
  }
}
