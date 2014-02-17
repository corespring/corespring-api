package org.corespring.platform.core.services.assessment.basic

import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao.{ SalatDAO, ModelCompanion }
import org.corespring.platform.core.models.assessment.basic.{ Answer, Question, Participant, Assessment }
import se.radley.plugin.salat._
import org.corespring.platform.core.models.itemSession.{DefaultItemSession, ItemSessionCompanion}
import com.mongodb.casbah.Imports._
import scala.Some
import org.joda.time.DateTime

trait AssessmentService {

  def addAnswer(assessmentId: ObjectId, externalUid: String, answer: Answer): Option[Assessment]
  def addParticipants(assessmentId: ObjectId, externalUids: Seq[String]): Option[Assessment]
  def create(q: Assessment): Unit
  def findAllByOrgId(id: ObjectId): List[Assessment]
  def findByIds(ids: List[ObjectId]): List[Assessment]
  def findByAuthor(authorId: String): List[Assessment]
  def findOneById(id: ObjectId): Option[Assessment]
  def remove(q: Assessment): Unit
  def update(q: Assessment): Unit
}

class AssessmentServiceImpl(itemSession: ItemSessionCompanion) extends AssessmentService {

  private object Keys {
    val orgId = "orgId"
    val authorId = "metadata.authorId"
  }

  /**
   * Hide the dao - it provides too many options
   * By hiding it we can thin out the client api for assessment
   */
  private object Dao extends ModelCompanion[Assessment, ObjectId] {

    import com.novus.salat.global._
    import play.api.Play.current
    import org.corespring.platform.core.models.mongoContext.context

    val collection = mongoCollection("assessments")
    val dao = new SalatDAO[Assessment, ObjectId](collection = collection){}
  }

  /** Bind Item title and standards to the question */
  private def bindItemData(q: Assessment): Assessment = {
    q.copy(questions = q.questions.map(Question.bindItemToQuestion))
  }

  def create(q: Assessment) {
    Dao.insert(bindItemData(q))
  }

  def update(q: Assessment) {
    Dao.save(bindItemData(q))
  }

  def count(query: DBObject = MongoDBObject(),
    fields: List[String] = List()): Long =
    Dao.count(query, fields)

  def removeAll() {
    Dao.remove(MongoDBObject())
  }

  def remove(q: Assessment) {
    Dao.remove(q)
  }

  def findOneById(id: ObjectId) = Dao.findOneById(id)

  def findByIds(ids: List[ObjectId]) = {
    val query = MongoDBObject("_id" -> MongoDBObject("$in" -> ids))
    Dao.find(query).toList
  }

  def collection = Dao.collection

  def findAllByOrgId(id: ObjectId): List[Assessment] = {
    val query = MongoDBObject(Keys.orgId -> id)
    Dao.find(query).toList
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
    withParticipantTimestamps(Dao.find(query).toList)
  }

  private def withParticipantTimestamps(assessments: List[Assessment]): List[Assessment] = {

    implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
    assessments.map(assessment => {
      assessment.copy(participants = assessment.participants.map(
        participant => {
          val timestamps = itemSession.find(
            MongoDBObject("_id" -> MongoDBObject("$in" -> participant.answers.map(_.sessionId)))).toList
            .map(_.dateModified)
          timestamps.nonEmpty match {
            case true => participant.copy(lastModified = timestamps.max)
            case _ => participant
          }
        }))
    })
  }
}

object AssessmentService extends AssessmentServiceImpl(DefaultItemSession)
