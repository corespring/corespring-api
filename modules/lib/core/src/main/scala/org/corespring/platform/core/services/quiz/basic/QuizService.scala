package org.corespring.platform.core.services.quiz.basic

import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import org.bson.types.ObjectId
import org.corespring.platform.core.models.quiz.basic.{Answer, Question, Participant, Quiz}
import scala.Some
import se.radley.plugin.salat._

trait QuizService{

  def addAnswer(quizId: ObjectId, externalUid: String, answer: Answer): Option[Quiz]
  def addParticipants(quizId: ObjectId, externalUids: Seq[String]): Option[Quiz]
  def create(q: Quiz) : Unit
  def findAllByOrgId(id: ObjectId): List[Quiz]
  def findByIds(ids: List[ObjectId]) : List[Quiz]
  def findOneById(id:ObjectId) : Option[Quiz]
  def remove(q: Quiz) : Unit
  def update(q: Quiz) : Unit
}

object QuizService extends QuizService{

  private object Keys {
    val orgId = "orgId"
  }

  /** Hide the dao - it provides too many options
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
      }
      else {
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
}