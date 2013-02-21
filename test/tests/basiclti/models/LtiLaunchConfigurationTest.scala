package tests.basiclti.models

import org.specs2.mutable.Specification
import basiclti.models._
import org.bson.types.ObjectId
import tests.PlaySingleton
import models.itemSession.ItemSessionSettings
import basiclti.models.LtiQuestion
import scala.Some

class LtiLaunchConfigurationTest extends Specification {

  PlaySingleton.start()

  "launch config" should {

    "add assignments" in {
      val config = new LtiQuiz(
        "1",
        LtiQuestion(Some(new ObjectId()), ItemSessionSettings()),
        Seq(),
        None)
      LtiQuiz.insert(config)

      config.participants.length === 0
      val updatedConfig = config.addParticipantIfNew("1", "passbackUrl", "finishedUrl")
      updatedConfig.participants.length === 1

      val anotherUpdate = updatedConfig.addParticipantIfNew("1", "?", "?")
      anotherUpdate.participants.length === 1

      val finalUpdate = anotherUpdate.addParticipantIfNew("2", "?", "?")
      finalUpdate.participants.length === 2
    }

    "can update works" in {

      val config = new LtiQuiz("1",
        LtiQuestion(Some(new ObjectId()), ItemSessionSettings()),
        Seq(),
        orgId = Some(new ObjectId()))
      LtiQuiz.insert(config)
      LtiQuiz.canUpdate(config, config.orgId.get) === true
      LtiQuiz.canUpdate(config, new ObjectId()) === false
    }

    "can't remove itemId if the config has assignments" in {

      val config = new LtiQuiz("1",
        LtiQuestion(Some(new ObjectId()), ItemSessionSettings()),
        Seq(LtiParticipant(new ObjectId(), "", "", "")),
        orgId = Some(new ObjectId())
      )
      LtiQuiz.insert(config)
      val copiedQuestion = config.question.copy(itemId = None)
      val copy = config.copy(question = copiedQuestion)
      LtiQuiz.canUpdate(copy, copy.orgId.get) === false
    }
  }

}
