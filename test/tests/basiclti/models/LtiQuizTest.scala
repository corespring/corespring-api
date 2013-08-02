package tests.basiclti.models

import basiclti.models._
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import tests.BaseTest
import org.corespring.platform.core.models.itemSession.ItemSessionSettings

class LtiQuizTest extends BaseTest {

  "launch config" should {

    "add assignments" in {
      val config = new LtiQuiz(
        "1",
        LtiQuestion(Some(VersionedId(new ObjectId())), ItemSessionSettings()),
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
        LtiQuestion(Some(VersionedId(ObjectId.get)), ItemSessionSettings()),
        Seq(),
        orgId = Some(ObjectId.get))
      LtiQuiz.insert(config)
      LtiQuiz.canUpdate(config, config.orgId.get) === true
      LtiQuiz.canUpdate(config, new ObjectId()) === false
    }

    "can't update the settings if somebody has participated with an item" in {

      val config = new LtiQuiz("1",
        LtiQuestion(Some(VersionedId(ObjectId.get)), ItemSessionSettings()),
        Seq(),
        orgId = Some(new ObjectId())
      )
      LtiQuiz.insert(config)
      LtiQuiz.canUpdate(config, config.orgId.get) === true
      val maybeUpdate = LtiQuiz.update(config.copy(participants = Seq(LtiParticipant(new ObjectId(), "", "", ""))), config.orgId.get)
      maybeUpdate.isRight === true
      val updatedQuiz: LtiQuiz = maybeUpdate.right.get

      val newSettings = updatedQuiz.question.settings.copy(maxNoOfAttempts = 2323)
      val newQuestion = updatedQuiz.question.copy(settings = newSettings)
      val newQuiz = updatedQuiz.copy(question = newQuestion)
      val maybeUpdateTwo = LtiQuiz.update(newQuiz, newQuiz.orgId.get)
      maybeUpdateTwo.isLeft === true
    }

    "can't remove itemId if the config has assignments" in {

      val config = new LtiQuiz("1",
        LtiQuestion(Some(VersionedId(ObjectId.get)), ItemSessionSettings()),
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
