package org.corespring.platform.core.models.quiz.basic

import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import org.corespring.platform.core.models.item.service.{ItemServiceImpl, ItemService, ItemServiceClient}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.Play.current
import play.api.libs.json.Json._
import play.api.libs.json._
import scala.Some
import se.radley.plugin.salat._
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.platform.core.models.itemSession.{ItemSessionSettings, DefaultItemSession, ItemSession}
import org.corespring.platform.core.models.quiz.{BaseQuestion, BaseParticipant, BaseQuiz}
import org.corespring.platform.core.models.versioning.VersionedIdImplicits
import org.corespring.platform.core.models.item.{TaskInfo, Item}

case class Answer(sessionId: ObjectId, itemId: ObjectId)

object Answer {

  implicit object Reads extends Reads[Answer] {
    override def reads(json: JsValue): JsResult[Answer] = {
      JsSuccess(Answer(new ObjectId((json \ "sessionId").as[String]),
        new ObjectId((json \ "itemId").as[String])
      ))
    }
  }

  implicit object Writes extends Writes[Answer] {
    def writes(a: Answer): JsValue = {

      val maybeSession: Option[ItemSession] = DefaultItemSession.findOneById(a.sessionId)

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
        val (score, total) = DefaultItemSession.getTotalScore(itemSession)
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
    def reads(json: JsValue): JsResult[Participant] = {
      JsSuccess(new Participant(
        (json \ "answers").as[Seq[Answer]],
        (json \ "externalUid").as[String]
      ))
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
case class Question(itemId: VersionedId[ObjectId],
                    settings: ItemSessionSettings = new ItemSessionSettings(),
                    title: Option[String] = None,
                    standards: Seq[String] = Seq()) extends BaseQuestion(Some(itemId), settings)

trait QuestionLike {
  self: ItemServiceClient =>

  implicit object Reads extends Reads[Question] {
    def reads(json: JsValue): JsResult[Question] = {

      import VersionedIdImplicits.{Reads => IdReads}

      JsSuccess(Question(
        (json \ "itemId").as[VersionedId[ObjectId]](IdReads),
        (json \ "settings").asOpt[ItemSessionSettings].getOrElse(ItemSessionSettings())
      ))
    }
  }

  implicit object Writes extends Writes[Question] {
    def writes(q: Question): JsValue = {

      import VersionedIdImplicits.{Writes =>IdWrites}
      JsObject(
        Seq(
          Some("itemId" -> toJson(q.itemId)(IdWrites)),
          if (q.settings != null) Some("settings" -> toJson(q.settings)) else None,
          q.title.map("title" -> JsString(_)),
          Some("standards" -> JsArray(q.standards.map(JsString(_))))
        ).flatten
      )
    }
  }

  def bindItemToQuestion(question: Question): Question = {
    itemService.findFieldsById(
      question.itemId,
      MongoDBObject("taskInfo.title" -> 1, "standards" -> 1)).toList.headOption match {
      case Some(dbo) => {
        import com.novus.salat._
        import com.mongodb.casbah.Imports._
        import org.corespring.platform.core.models.mongoContext.context
        val item : Item = grater[Item].asObject(dbo)
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

object Question extends QuestionLike with ItemServiceClient {
  def itemService: ItemService = ItemServiceImpl
}

case class Quiz(orgId: Option[ObjectId] = None,
                metadata: Map[String, String] = Map(),
                questions: Seq[Question] = Seq(),
                participants: Seq[Participant] = Seq(),
                id: ObjectId = new ObjectId()) extends BaseQuiz(questions, participants, id)


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
    def reads(json: JsValue): JsResult[Quiz] = {

      val participants = (json \ "participants").asOpt[Seq[Participant]].getOrElse(Seq())

      JsSuccess(Quiz(
        (json \ "orgId").asOpt[String].map(new ObjectId(_)),
        (json \ "metadata").asOpt[Map[String, String]].getOrElse(Map()),
        (json \ "questions").asOpt[Seq[Question]].getOrElse(Seq()),
        participants,
        (json \ "id").asOpt[String].map(new ObjectId(_)).getOrElse(new ObjectId())
      ))
    }
  }

  /** Hide the dao - it provides too many options
    * By hiding it we can thin out the client api for quiz
    */
  private object Dao extends ModelCompanion[Quiz, ObjectId] {
    import play.api.Play.current
    val collection = mongoCollection("quizzes")
    import org.corespring.platform.core.models.mongoContext.context
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
      case Some(q) => addParticipants(q, externalUids)
      case None => None
    }
  }

  def addParticipants(q: Quiz, externalUids: Seq[String]): Option[Quiz] = {
    val updatedQuiz = q.copy(participants = q.participants ++ externalUids.map(euid => Participant(Seq(), euid)))
    Quiz.update(updatedQuiz)
    Some(updatedQuiz)
  }

}
