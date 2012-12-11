package tests.basiclti.models

import org.specs2.mutable.Specification
import basiclti.models.LtiLaunchConfiguration
import org.bson.types.ObjectId
import tests.PlaySingleton

class LtiLaunchConfigurationTest extends Specification{

  PlaySingleton.start()

  "launch config" should {

    "add assignments" in {
      val config = new LtiLaunchConfiguration("1", Some(new ObjectId()), None, None)

      config.assignments.length === 0
      val updatedConfig = config.addAssignmentIfNew("1", "passbackUrl", "finishedUrl")
      updatedConfig.assignments.length === 1

      val anotherUpdate = updatedConfig.addAssignmentIfNew("1", "?", "?")
      anotherUpdate.assignments.length === 1

      val finalUpdate = anotherUpdate.addAssignmentIfNew("2", "?", "?")

      finalUpdate.assignments.length === 2
    }
  }

}
