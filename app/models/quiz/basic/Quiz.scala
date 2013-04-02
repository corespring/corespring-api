package models.quiz.basic

import org.bson.types.ObjectId
import models.itemSession.{ItemSession, ItemSessionSettings}
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import se.radley.plugin.salat._
import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import play.api.Play.current
import models.quiz.{BaseParticipant, BaseQuestion}
import play.api.libs.json._
import play.api.libs.json.Json._
import models.mongoContext._
import models.item.{TaskInfo, Item}
import play.api.libs.json.JsObject
import scala.Some
import org.joda.time.DateTime

case class Answer(sessionId: ObjectId, itemId: ObjectId)

object Answer {

  implicit object Reads extends Reads[Answer] {
    def reads(json: JsValue): Answer = {
      Answer(new ObjectId((json \ "sessionId").as[String]),
        new ObjectId((json \ "itemId").as[String])
      )
    }
  }

  implicit object Writes extends Writes[Answer] {
    def writes(a: Answer): JsValue = {

      val maybeSession: Option[ItemSession] = ItemSession.findOneById(a.sessionId)

      JsObject(Seq(
        "sessionId" -> JsString(a.sessionId.toString),
        "itemId" -> JsString(a.itemId.toString),
        "score" -> JsNumber(calculateScore(maybeSession)),
        "lastResponse" -> JsNumber(getLastResponse(maybeSession)),
        "isComplete" -> JsBoolean(isComplete(maybeSession))
      ))
    }
  }

  /** Return the last response time stamp. Use in order of preference dateModified, finish, start, id.time */
  private def getLastResponse(session: Option[ItemSession]): Long = session match {
    case Some(s) => Seq(s.dateModified, s.finish, s.start, Some(new DateTime(s.id.getTime))).flatten.head.getMillis
    case _ => -1
  }

  private def calculateScore(maybeSession: Option[ItemSession]): Int = maybeSession match {
    case Some(itemSession) => {
      if (itemSession.isFinished) {
        val (score, total) = ItemSession.getTotalScore(itemSession)
        if (score == 0) 0 else ((score / total) * 100).toInt
      } else 0
    }
    case None => 0
  }

  private def isComplete(maybeSession: Option[ItemSession]): Boolean = maybeSession match {
    case Some(itemSession) => itemSession.isFinished
    case None => false
  }
}

case class Participant(answers: Seq[Answer],
                       externalUid: String) extends BaseParticipant(answers.map(_.sessionId), externalUid)

object Participant {

  implicit object Reads extends Reads[Participant] {
    def reads(json: JsValue): Participant = {
      new Participant(
        (json \ "answers").as[Seq[Answer]],
        (json \ "externalUid").as[String]
      )
    }
  }

  implicit object Writes extends Writes[Participant] {
    def writes(p: Participant): JsValue = {
      JsObject(Seq(
        "answers" -> toJson(p.answers),
        "externalUid" -> JsString(p.externalUid)
      ))
    }
  }

}

/** Note: We are adding the title and standard info from the Item model here.
  * Its a little bit of duplication - but will save us from having to make a 2nd db query
  */
case class Question(itemId: ObjectId,
                    settings: ItemSessionSettings = ItemSessionSettings(),
                    title: Option[String] = None,
                    standards: Seq[String] = Seq()) extends BaseQuestion(Some(itemId), settings)

object Question {

  implicit object Reads extends Reads[Question] {
    def reads(json: JsValue): Question = {
      Question(
        new ObjectId((json \ "itemId").as[String]),
        (json \ "settings").asOpt[ItemSessionSettings].getOrElse(ItemSessionSettings())
      )
    }
  }

  implicit object Writes extends Writes[Question] {
    def writes(q: Question): JsValue = {

      JsObject(
        Seq(
          Some("itemId" -> JsString(q.itemId.toString)),
          if (q.settings != null) Some("settings" -> toJson(q.settings)) else None,
          q.title.map("title" -> JsString(_)),
          Some("standards" -> JsArray(q.standards.map(JsString(_))))
        ).flatten
      )
    }
  }

  def bindItemToQuestion(question: Question): Question = {
    Item.find(
      MongoDBObject("_id" -> question.itemId),
      MongoDBObject("taskInfo.title" -> 1, "standards" -> 1)).toList.headOption match {
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
}

case class Quiz(orgId: Option[ObjectId] = None,
                metadata: Map[String, String] = Map(),
                questions: Seq[Question] = Seq(),
                participants: Seq[Participant] = Seq(),
                id: ObjectId = new ObjectId()) extends models.quiz.BaseQuiz(questions, participants, id)


object Quiz {

  private object Keys {
    val orgId = "orgId"
  }

  implicit object Writes extends Writes[Quiz] {
    def writes(q: Quiz): JsObject = {

      val props = List(
        Some("id" -> JsString(q.id.toString)),
        q.orgId.map((o: ObjectId) => ("orgId" -> JsString(o.toString))), //,
        Some("metadata" -> toJson(q.metadata)),
        Some("participants" -> toJson(q.participants)),
        Some("questions" -> toJson(q.questions))
      ).flatten

      JsObject(props)
    }
  }

  implicit object Reads extends Reads[Quiz] {
    def reads(json: JsValue): Quiz = {

      val participants = (json \ "participants").asOpt[Seq[Participant]].getOrElse(Seq())

      Quiz(
        (json \ "orgId").asOpt[String].map(new ObjectId(_)),
        (json \ "metadata").asOpt[Map[String, String]].getOrElse(Map()),
        (json \ "questions").asOpt[Seq[Question]].getOrElse(Seq()),
        participants,
        (json \ "id").asOpt[String].map(new ObjectId(_)).getOrElse(new ObjectId())
      )
    }
  }

  /** Hide the dao - it provides too many options
    * By hiding it we can thin out the client api for quiz
    */
  private object Dao extends ModelCompanion[Quiz, ObjectId] {
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

    Quiz.findOneById(quizId) match {
      case Some(q) => {
        val updatedQuiz = q.copy(participants = q.participants.map(processParticipants(externalUid)))
        Quiz.update(updatedQuiz)
        Some(updatedQuiz)
      }
      case None => None
    }
  }

  def addParticipants(quizId: ObjectId, externalUids: Seq[String]): Option[Quiz] = {
    Quiz.findOneById(quizId) match {
      case Some(q) => {
        val updatedQuiz = q.copy(participants = q.participants ++ externalUids.map(euid => Participant(Seq(), euid)))
        Quiz.update(updatedQuiz)
        Some(updatedQuiz)
      }
      case None => None
    }
  }

}
