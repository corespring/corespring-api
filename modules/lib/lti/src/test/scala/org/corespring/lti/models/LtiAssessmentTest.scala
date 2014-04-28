package org.corespring.lti.models

import org.bson.types.ObjectId
import org.corespring.platform.core.models.itemSession.ItemSessionSettings
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.BaseTest
import scala.Some

class LtiAssessmentTest extends BaseTest {

  "launch config" should {

    "add assignments" in {
      val config = new LtiAssessment(
        "1",
        LtiQuestion(Some(VersionedId(new ObjectId())), ItemSessionSettings()),
        Seq(),
        None)
      LtiAssessment.insert(config)

      config.participants.length === 0
      val updatedConfig = config.addParticipantIfNew("1", "passbackUrl", "finishedUrl")
      updatedConfig.participants.length === 1

      val anotherUpdate = updatedConfig.addParticipantIfNew("1", "?", "?")
      anotherUpdate.participants.length === 1

      val finalUpdate = anotherUpdate.addParticipantIfNew("2", "?", "?")
      finalUpdate.participants.length === 2
    }

    "can update works" in {

      val config = new LtiAssessment("1",
        LtiQuestion(Some(VersionedId(ObjectId.get)), ItemSessionSettings()),
        Seq(),
        orgId = Some(ObjectId.get))
      LtiAssessment.insert(config)
      LtiAssessment.canUpdate(config, config.orgId.get) === true
      LtiAssessment.canUpdate(config, new ObjectId()) === false
    }

    "can't update the settings if somebody has participated with an item" in {

      val config = new LtiAssessment("1",
        LtiQuestion(Some(VersionedId(ObjectId.get)), ItemSessionSettings()),
        Seq(),
        orgId = Some(new ObjectId())
      )
      LtiAssessment.insert(config)
      LtiAssessment.canUpdate(config, config.orgId.get) === true
      val maybeUpdate = LtiAssessment.update(config.copy(participants = Seq(LtiParticipant(new ObjectId(), "", "", ""))), config.orgId.get)
      maybeUpdate.isRight === true
      val updatedAssessment: LtiAssessment = maybeUpdate.right.get

      val newSettings = updatedAssessment.question.settings.copy(maxNoOfAttempts = 2323)
      val newQuestion = updatedAssessment.question.copy(settings = newSettings)
      val newAssessment = updatedAssessment.copy(question = newQuestion)
      val maybeUpdateTwo = LtiAssessment.update(newAssessment, newAssessment.orgId.get)
      maybeUpdateTwo.isLeft === true
    }

    "can't remove itemId if the config has assignments" in {

      val config = new LtiAssessment("1",
        LtiQuestion(Some(VersionedId(ObjectId.get)), ItemSessionSettings()),
        Seq(LtiParticipant(new ObjectId(), "", "", "")),
        orgId = Some(new ObjectId())
      )
      LtiAssessment.insert(config)
      val copiedQuestion = config.question.copy(itemId = None)
      val copy = config.copy(question = copiedQuestion)
      LtiAssessment.canUpdate(copy, copy.orgId.get) === false
    }
  }

}
