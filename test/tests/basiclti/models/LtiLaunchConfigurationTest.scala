package tests.basiclti.models

import org.specs2.mutable.Specification
import basiclti.models.{Assignment, LtiLaunchConfiguration}
import org.bson.types.ObjectId
import tests.{BaseTest, PlaySingleton}

class LtiLaunchConfigurationTest extends BaseTest{

  "launch config" should {

    "add assignments" in {
      val config = new LtiLaunchConfiguration("1", Some(new ObjectId()), None, None)
      LtiLaunchConfiguration.insert(config)

      config.assignments.length === 0
      val updatedConfig = config.addAssignmentIfNew("1", "passbackUrl", "finishedUrl")
      updatedConfig.assignments.length === 1

      val anotherUpdate = updatedConfig.addAssignmentIfNew("1", "?", "?")
      anotherUpdate.assignments.length === 1

      val finalUpdate = anotherUpdate.addAssignmentIfNew("2", "?", "?")

      finalUpdate.assignments.length === 2
    }

    "can update works" in {

      val config = new LtiLaunchConfiguration("1", Some(new ObjectId()), None, orgId = Some(new ObjectId()))
      LtiLaunchConfiguration.insert(config)

      LtiLaunchConfiguration.canUpdate(config, config.orgId.get) === true
      LtiLaunchConfiguration.canUpdate(config, new ObjectId()) === false
    }

    "can't remove itemId if the config has assignments" in {

      val assignments = Seq( Assignment("1", new ObjectId(), "", "" ))
      val config = new LtiLaunchConfiguration("1",
        Some(new ObjectId()),
        None,
        orgId = Some(new ObjectId()),
        assignments = assignments
      )
      LtiLaunchConfiguration.insert(config)
      val copy = config.copy(itemId = None)
      LtiLaunchConfiguration.canUpdate(copy, copy.orgId.get) === false
    }
  }

}
