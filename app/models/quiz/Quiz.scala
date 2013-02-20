package models.quiz

import org.bson.types.ObjectId
import models.itemSession.ItemSessionSettings

abstract class BaseParticipant(itemSessions: Seq[ObjectId], uid: String)

abstract class BaseQuestion(itemId: ObjectId, settings: ItemSessionSettings)

abstract class BaseQuiz(questions: Seq[BaseQuestion] = Seq(),
                           participants: Seq[BaseParticipant] = Seq(),
                           id: ObjectId = new ObjectId())

