package org.corespring.platform.core.models.quiz

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.platform.core.models.itemSession.ItemSessionSettings

abstract class BaseParticipant(itemSessions: Seq[ObjectId], uid: String)

abstract class BaseQuestion(itemId: Option[VersionedId[ObjectId]], settings: ItemSessionSettings)

abstract class BaseQuiz(questions: Seq[BaseQuestion] = Seq(),
                           participants: Seq[BaseParticipant] = Seq(),
                           id: ObjectId = new ObjectId())

