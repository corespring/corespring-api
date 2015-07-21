package org.corespring.models.assessment

import org.bson.types.ObjectId
import org.corespring.models.itemSession.ItemSessionSettings
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.DateTime

abstract class BaseParticipant(itemSessions: Seq[ObjectId], uid: String)

abstract class BaseQuestion(itemId: Option[VersionedId[ObjectId]], settings: ItemSessionSettings)

abstract class BaseAssessment(questions: Seq[BaseQuestion] = Seq(),
  participants: Seq[BaseParticipant] = Seq(),
  id: ObjectId = new ObjectId())

case class Answer(sessionId: ObjectId, itemId: VersionedId[ObjectId])

case class Participant(answers: Seq[Answer],
  externalUid: String, lastModified: Option[DateTime] = None) extends BaseParticipant(answers.map(_.sessionId), externalUid)
case class Assessment(orgId: Option[ObjectId] = None,
    metadata: Map[String, String] = Map(),
    questions: Seq[Question] = Seq(),
    starts: Option[DateTime] = None,
    ends: Option[DateTime] = None,
    participants: Seq[Participant] = Seq(),
    id: ObjectId = new ObjectId()) extends BaseAssessment(questions, participants, id) {

  def merge(that: Assessment) = this.copy(
    orgId = if (that.orgId.nonEmpty) that.orgId else this.orgId,
    participants = if (that.participants.length > 0) that.participants else this.participants,
    questions = if (that.questions.length > 0) that.questions else this.questions,
    metadata = if (that.metadata.size > 0) that.metadata else this.metadata)
}

case class Question(itemId: VersionedId[ObjectId],
  settings: ItemSessionSettings = new ItemSessionSettings(),
  title: Option[String] = None,
  standards: Seq[String] = Seq()) extends BaseQuestion(Some(itemId), settings)
