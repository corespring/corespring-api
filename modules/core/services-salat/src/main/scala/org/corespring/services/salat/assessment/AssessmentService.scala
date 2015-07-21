package org.corespring.services.salat.assessment

import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.models.assessment.{ Participant, Answer, Question, Assessment }
import org.corespring.models.item.TaskInfo
import org.corespring.services.salat.HasDao
import org.corespring.{ services => interface }
/**
 * Note: Assessments were using the old v1 itemSession.
 * Need to glue in the new item session instead.
 */
trait AssessmentService extends interface.assessment.AssessmentService with HasDao[Assessment, ObjectId] {

  def itemService: interface.item.ItemService

  private object Keys {
    val orgId = "orgId"
    val authorId = "metadata.authorId"
  }

  def bindItemToQuestion(question: Question): Question = {
    //TODO: RF: The look up here trimmed the fields to speed up the serialization - worth exposing on itemService?
    itemService.findOneById(question.itemId) match {
      case Some(item) => {
        val title = item.taskInfo.getOrElse(TaskInfo(title = Some(""))).title
        val standards = item.standards
        question.copy(
          title = title,
          standards = standards)
      }
      case _ => question
    }
  }

  /** Bind Item title and standards to the question */
  private def bindItemData(q: Assessment): Assessment = {
    q.copy(questions = q.questions.map(bindItemToQuestion))
  }

  def create(q: Assessment) {
    dao.insert(bindItemData(q))
  }

  def update(q: Assessment) {
    dao.save(bindItemData(q))
  }

  def count(query: DBObject = MongoDBObject(),
    fields: List[String] = List()): Long =
    dao.count(query, fields)

  def removeAll() {
    dao.remove(MongoDBObject())
  }

  def remove(q: Assessment) {
    dao.remove(q)
  }

  def findOneById(id: ObjectId) = dao.findOneById(id)

  def findByIdAndOrg(id: ObjectId, organizationId: ObjectId) = {
    val query = MongoDBObject("_id" -> id, "orgId" -> organizationId)
    dao.findOne(query)
  }

  def findByIds(ids: List[ObjectId]) = {
    val query = MongoDBObject("_id" -> MongoDBObject("$in" -> ids))
    dao.find(query).toList
  }

  def findByIds(ids: List[ObjectId], organizationId: ObjectId) = {
    val query = MongoDBObject("_id" -> MongoDBObject("$in" -> ids), "orgId" -> organizationId)
    dao.find(query).toList
  }

  def collection = dao.collection

  def findAllByOrgId(id: ObjectId): List[Assessment] = {
    val query = MongoDBObject(Keys.orgId -> id)
    dao.find(query).toList
  }

  def addAnswer(assessmentId: ObjectId, externalUid: String, answer: Answer): Option[Assessment] = {

    def processParticipants(externalUid: String)(p: Participant): Participant = {
      if (p.externalUid == externalUid && !p.answers.exists(_.itemId == answer.itemId)) {
        p.copy(answers = p.answers :+ answer)
      } else {
        p
      }
    }

    findOneById(assessmentId) match {
      case Some(q) => {
        val updatedAssessment = q.copy(participants = q.participants.map(processParticipants(externalUid)))
        update(updatedAssessment)
        Some(updatedAssessment)
      }
      case None => None
    }
  }

  def addParticipants(assessmentId: ObjectId, externalUids: Seq[String]): Option[Assessment] = {
    findOneById(assessmentId) match {
      case Some(q) => addParticipants(q, externalUids)
      case None => None
    }
  }

  def addParticipants(q: Assessment, externalUids: Seq[String]): Option[Assessment] = {
    val updatedAssessment = q.copy(participants = q.participants ++ externalUids.map(euid => Participant(Seq(), euid)))
    update(updatedAssessment)
    Some(updatedAssessment)
  }

  def findByAuthor(authorId: String): List[Assessment] = {
    val query = MongoDBObject(Keys.authorId -> authorId)
    //withParticipantTimestamps(dao.find(query).toList)
    dao.find(query).toList
  }

  def findByAuthorAndOrg(authorId: String, organizationId: ObjectId): List[Assessment] = {
    val query = MongoDBObject(Keys.authorId -> authorId, Keys.orgId -> organizationId)
    dao.find(query).toList
    //withParticipantTimestamps(dao.find(query).toList)
  }

  /*private def withParticipantTimestamps(assessments: List[Assessment]): List[Assessment] = {

    implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
    assessments.map(assessment => {
      assessment.copy(participants = assessment.participants.map(
        participant => {
          val sessions = itemSessionService.findMultiple(participant.answers.map(_.sessionId))
          val timestamps = sessions.map(_.dateModified)
          timestamps.nonEmpty match {
            case true => participant.copy(lastModified = timestamps.max)
            case _ => participant
          }
        }))
    })
  }*/
}

