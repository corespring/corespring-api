package org.corespring.platform.core.models.assessment.basic

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import org.corespring.platform.core.models.assessment._
import org.corespring.platform.core.models.item.{ TaskInfo, Item }
import org.corespring.platform.core.models.itemSession._
import org.corespring.platform.core.models.json.JsonValidationException
import org.corespring.platform.core.models.assessment._
import org.corespring.platform.core.models.versioning.VersionedIdImplicits
import org.corespring.platform.core.services.item._
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.DateTime
import play.api.libs.json.Json._
import play.api.libs.json._
import scala.Some

case class Answer(sessionId: ObjectId, itemId: VersionedId[ObjectId])

object Answer {

  implicit object Reads extends Reads[Answer] {

    import VersionedIdImplicits.{ Reads => IdReads }

    override def reads(json: JsValue): JsResult[Answer] = {
      JsSuccess(
        Answer(
          sessionId = new ObjectId((json \ "sessionId").as[String]),
          itemId = (json \ "itemId").asOpt[VersionedId[ObjectId]](IdReads)
            .getOrElse(throw new JsonValidationException("You must have an item id"))))
    }
  }

  implicit object Writes extends Writes[Answer] {

    import VersionedIdImplicits.{ Writes => IdWrites }

    def writes(a: Answer): JsValue = {

      val maybeSession: Option[ItemSession] = DefaultItemSession.findOneById(a.sessionId)

      JsObject(Seq(
        "sessionId" -> JsString(a.sessionId.toString),
        "itemId" -> Json.toJson(a.itemId)(IdWrites),
        "score" -> JsNumber(calculateScore(maybeSession)),
        "lastResponse" -> JsNumber(getLastResponse(maybeSession)),
        "isComplete" -> JsBoolean(isComplete(maybeSession))))
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
  externalUid: String, lastModified: Option[DateTime] = None) extends BaseParticipant(answers.map(_.sessionId), externalUid)

object Participant {

  implicit object Reads extends Reads[Participant] {
    def reads(json: JsValue): JsResult[Participant] = {
      JsSuccess(new Participant(
        (json \ "answers").as[Seq[Answer]],
        (json \ "externalUid").as[String]))
    }
  }

  implicit object Writes extends Writes[Participant] {
    def writes(p: Participant): JsValue = {
      val fields = Seq(
        "answers" -> toJson(p.answers),
        "externalUid" -> JsString(p.externalUid))
      p.lastModified match {
        case None => JsObject(fields)
        case Some(time) => JsObject(fields ++ Seq(("lastModified" -> JsNumber(time.getMillis))))
      }
    }
  }

}

/**
 * Note: We are adding the title and standard info from the Item model here.
 * Its a little bit of duplication - but will save us from having to make a 2nd db query
 */
case class Question(itemId: VersionedId[ObjectId],
  settings: ItemSessionSettings = new ItemSessionSettings(),
  title: Option[String] = None,
  standards: Seq[String] = Seq()) extends BaseQuestion(Some(itemId), settings)

trait QuestionLike {
  self: ItemServiceClient =>

  implicit val QuestionReads = Question.Format

  def bindItemToQuestion(question: Question): Question = {
    itemService.findFieldsById(
      question.itemId,
      MongoDBObject("taskInfo.title" -> 1, "standards" -> 1)).toList.headOption match {
        case Some(dbo) => {
          import com.mongodb.casbah.Imports._
          import com.novus.salat._
          import org.corespring.platform.core.models.mongoContext.context
          val item: Item = grater[Item].asObject(dbo)
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
  def itemService: ItemService = ItemServiceWired

  object Format extends Format[Question] {
    def reads(json: JsValue): JsResult[Question] = {

      import VersionedIdImplicits.{ Reads => IdReads }

      JsSuccess(Question(
        (json \ "itemId").as[VersionedId[ObjectId]](IdReads),
        (json \ "settings").asOpt[ItemSessionSettings].getOrElse(ItemSessionSettings())))
    }

    def writes(q: Question): JsValue = {

      import VersionedIdImplicits.{ Writes => IdWrites }
      JsObject(
        Seq(
          Some("itemId" -> toJson(q.itemId)(IdWrites)),
          if (q.settings != null) Some("settings" -> toJson(q.settings)) else None,
          q.title.map("title" -> JsString(_)),
          Some("standards" -> JsArray(q.standards.map(JsString(_))))).flatten)
    }
  }

}

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

object Assessment {

  implicit object Writes extends Writes[Assessment] {
    def writes(q: Assessment): JsObject = {

      val props = List(
        Some("id" -> JsString(q.id.toString)),
        q.orgId.map((o: ObjectId) => ("orgId" -> JsString(o.toString))),
        Some("metadata" -> toJson(q.metadata)),
        Some("start" -> toJson(q.starts)),
        Some("end" -> toJson(q.ends)),
        Some("participants" -> toJson(q.participants)),
        Some("questions" -> toJson(q.questions))).flatten

      JsObject(props)
    }
  }

  implicit object Reads extends Reads[Assessment] {
    def reads(json: JsValue): JsResult[Assessment] = {

      val participants = (json \ "participants").asOpt[Seq[Participant]].getOrElse(Seq())

      JsSuccess(Assessment(
        (json \ "orgId").asOpt[String].map(new ObjectId(_)),
        (json \ "metadata").asOpt[Map[String, String]].getOrElse(Map()),
        (json \ "questions").asOpt[Seq[Question]].getOrElse(Seq()),
        (json \ "start").asOpt[DateTime],
        (json \ "end").asOpt[DateTime],
        participants,
        (json \ "id").asOpt[String].map(new ObjectId(_)).getOrElse(new ObjectId())))
    }
  }

}