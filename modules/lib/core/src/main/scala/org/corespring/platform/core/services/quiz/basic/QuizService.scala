package org.corespring.platform.core.services.quiz.basic

import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao.{ SalatDAO, ModelCompanion }
import org.corespring.platform.core.models.quiz.basic.{ Answer, Question, Participant, Quiz }
import se.radley.plugin.salat._
import org.corespring.platform.core.models.itemSession.{DefaultItemSession, ItemSessionCompanion}
import com.mongodb.casbah.Imports._
import scala.Some
import org.joda.time.DateTime

trait QuizService {

  def addAnswer(quizId: ObjectId, externalUid: String, answer: Answer): Option[Quiz]
  def addParticipants(quizId: ObjectId, externalUids: Seq[String]): Option[Quiz]
  def create(q: Quiz): Unit
  def findAllByOrgId(id: ObjectId): List[Quiz]
  def findByIds(ids: List[ObjectId]): List[Quiz]
  def findByAuthor(authorId: String): List[Quiz]
  def findOneById(id: ObjectId): Option[Quiz]
  def remove(q: Quiz): Unit
  def update(q: Quiz): Unit
}

class QuizServiceImpl(itemSession: ItemSessionCompanion) extends QuizService {

  private object Keys {
    val orgId = "orgId"
    val authorId = "metadata.authorId"
  }

  /**
   * Hide the dao - it provides too many options
   * By hiding it we can thin out the client api for quiz
   */
  private object Dao extends ModelCompanion[Quiz, ObjectId] {

    import org.corespring.platform.core.models.mongoContext.context
    import play.api.Play.current

    val collection = mongoCollection("quizzes")
    val dao = new SalatDAO[Quiz, ObjectId](collection = collection) {}
  }

  /** Bind Item title and standards to the question */
  private def bindItemData(q: Quiz): Quiz = {
    q.copy(questions = q.questions.map(Question.bindItemToQuestion))
  }

  def create(q: Quiz) {
    Dao.save(bindItemData(q))
  }

  def update(q: Quiz) {
    Dao.save(bindItemData(q))
  }

  def count(query: DBObject = MongoDBObject(),
    fields: List[String] = List()): Long =
    Dao.count(query, fields)

  def removeAll() {
    Dao.remove(MongoDBObject())
  }

  def remove(q: Quiz) {
    Dao.remove(q)
  }

  def findOneById(id: ObjectId) = Dao.findOneById(id)

  def findByIds(ids: List[ObjectId]) = {
    val query = MongoDBObject("_id" -> MongoDBObject("$in" -> ids))
    Dao.find(query).toList
  }

  def collection = Dao.collection

  def findAllByOrgId(id: ObjectId): List[Quiz] = {
    val query = MongoDBObject(Keys.orgId -> id)
    Dao.find(query).toList
  }

  def addAnswer(quizId: ObjectId, externalUid: String, answer: Answer): Option[Quiz] = {

    def processParticipants(externalUid: String)(p: Participant): Participant = {
      if (p.externalUid == externalUid && !p.answers.exists(_.itemId == answer.itemId)) {
        p.copy(answers = p.answers :+ answer)
      } else {
        p
      }
    }

    findOneById(quizId) match {
      case Some(q) => {
        val updatedQuiz = q.copy(participants = q.participants.map(processParticipants(externalUid)))
        update(updatedQuiz)
        Some(updatedQuiz)
      }
      case None => None
    }
  }

  def addParticipants(quizId: ObjectId, externalUids: Seq[String]): Option[Quiz] = {
    findOneById(quizId) match {
      case Some(q) => addParticipants(q, externalUids)
      case None => None
    }
  }

  def addParticipants(q: Quiz, externalUids: Seq[String]): Option[Quiz] = {
    val updatedQuiz = q.copy(participants = q.participants ++ externalUids.map(euid => Participant(Seq(), euid)))
    update(updatedQuiz)
    Some(updatedQuiz)
  }

  def findByAuthor(authorId: String): List[Quiz] = {
    val query = MongoDBObject(Keys.authorId -> authorId)
    withParticipantTimestamps(Dao.find(query).toList)
  }

  private def withParticipantTimestamps(quizzes: List[Quiz]): List[Quiz] = {

    implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
    quizzes.map(quiz => {
      quiz.copy(participants = quiz.participants.map(
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

object QuizService extends QuizServiceImpl(DefaultItemSession)
