package models.quiz.basic

import org.bson.types.ObjectId
import models.itemSession.ItemSessionSettings
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import se.radley.plugin.salat._
import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import play.api.Play.current
import models.mongoContext._
import models.quiz.{BaseParticipant, BaseQuestion}
import play.api.libs.json.{JsObject, Writes, JsValue, Reads}
import common.models.json.jerkson.{JerksonReads, JerksonWrites}

case class Participant(itemSessions: Seq[ObjectId],
                       externalUid: String,
                       metadata: Map[String, String]) extends BaseParticipant(itemSessions, externalUid)

case class Question(itemId: ObjectId,
                    settings: ItemSessionSettings) extends BaseQuestion(itemId, settings)

case class Quiz(orgId: Option[ObjectId] = None,
                metadata: Map[String, String] = Map(),
                questions: Seq[Question] = Seq(),
                participants: Seq[Participant] = Seq(),
                id: ObjectId = new ObjectId()) extends models.quiz.BaseQuiz(questions, participants, id)


object Quiz {

  implicit object Writes extends JerksonWrites[Quiz]

  implicit object Reads extends JerksonReads[Quiz] {
    def manifest = Manifest.classType(new Quiz().getClass)
  }

  /** Hide the dao - it provides too many options
    * By hiding it we can thin out the client api for quiz
    */
  private object Dao extends ModelCompanion[Quiz, ObjectId] {
    val collection = mongoCollection("quizzes")
    val dao = new SalatDAO[Quiz, ObjectId](collection = collection) {}
  }

  def create(q: Quiz) {
    Dao.save(q)
  }

  def update(q : Quiz) {
    Dao.save(q)
  }

  def count(query: DBObject = MongoDBObject(),
            fields: List[String] = List()): Long =
    Dao.count(query, fields)

  def removeAll() {
    Dao.remove(MongoDBObject())
  }

  def remove(q:Quiz) {
    Dao.remove(q)
  }

  def findOneById(id: ObjectId) = Dao.findOneById(id)

  def collection = Dao.collection
}
