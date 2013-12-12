package org.corespring.lti.models

import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao._
import org.bson.types.ObjectId
import org.corespring.platform.core.models.itemSession.{ ItemSessionSettings, DefaultItemSession, ItemSession }
import org.corespring.platform.core.models.quiz.{ BaseQuiz, BaseQuestion, BaseParticipant }
import org.corespring.platform.core.models.versioning.VersionedIdImplicits._
import org.corespring.platform.core.models.versioning.VersionedIdImplicits
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.platform.core.models.json.{ObjectIdReads, ObjectIdWrites}
import play.api.Play.current
import play.api.libs.json._
import scala.{Boolean, Left, Right, Some}
import se.radley.plugin.salat._
import org.corespring.api.v1.errors.ApiError

case class LtiQuestion(itemId: Option[VersionedId[ObjectId]],
  settings: ItemSessionSettings)
  extends BaseQuestion(itemId, settings)

object LtiQuestion {

  implicit object Writes extends Writes[LtiQuestion] {

    import VersionedIdImplicits.{ Writes => IdWrites }

    def writes(q: LtiQuestion): JsValue = {
      val out = Seq("settings" -> Json.toJson(q.settings)) ++ q.itemId.map(id => "itemId" -> Json.toJson(id)(IdWrites))
      JsObject(out)
    }
  }

  implicit val idReads = VersionedIdImplicits.Reads

  implicit val LtiQuestionReads = Json.reads[LtiQuestion]
}

case class LtiParticipant(itemSession: ObjectId,
  resultSourcedId: String,
  gradePassbackUrl: String,
  onFinishedUrl: String)
  extends BaseParticipant(Seq(itemSession), resultSourcedId)

object LtiParticipant {

  implicit val idReads = VersionedIdImplicits.Reads
  implicit val idWrites = VersionedIdImplicits.Writes
  implicit val Writes = Json.writes[LtiParticipant]
  implicit val Reads = Json.reads[LtiParticipant]
}

case class LtiQuiz(resourceLinkId: String,
  question: LtiQuestion,
  participants: Seq[LtiParticipant] = Seq(),
  orgId: Option[ObjectId],
  id: ObjectId = new ObjectId())
  extends BaseQuiz(Seq(question), participants, id) {

  def hasAssignments = this.participants.length > 0

  def addParticipantIfNew(resultSourcedId: String,
    passbackUrl: String,
    finishedUrl: String): LtiQuiz = {

    participants.find(_.resultSourcedId == resultSourcedId) match {
      case Some(a) => this
      case _ => {

        require(question.itemId != null, "No itemId is defined")

        val session = new ItemSession(
          itemId = question.itemId.get,
          settings = question.settings)
        DefaultItemSession.save(session)

        val newParticipant = LtiParticipant(
          itemSession = session.id,
          resultSourcedId = resultSourcedId,
          gradePassbackUrl = passbackUrl,
          onFinishedUrl = finishedUrl)
        val updated = this.participants :+ newParticipant

        val updatedQuiz = this.copy(participants = updated)

        LtiQuiz.updateNoValidation(updatedQuiz) match {
          case Left(e) => throw new RuntimeException("Error updating LtiQuizzes")
          case Right(q) => q
        }
      }
    }
  }
}

object LtiQuiz {

  implicit val idReads = VersionedIdImplicits.Reads
  implicit val Writes = Json.writes[LtiQuiz]
  implicit val Reads = Json.reads[LtiQuiz]

  /**
   * Hide the ModelCompanion from the client code as it provides too many operations.
   */
  private object Dao extends ModelCompanion[LtiQuiz, ObjectId] {
    val collection = mongoCollection("lti_quizzes")

    import org.corespring.platform.core.models.mongoContext.context

    val dao = new SalatDAO[LtiQuiz, ObjectId](collection = collection) {}
  }

  def collection = Dao.collection

  private def updateNoValidation(update: LtiQuiz): Either[ApiError, LtiQuiz] = Dao.findOneById(update.id) match {
    case Some(c) => {
      Dao.save(update)
      Right(update)
    }
    case _ => Left(new ApiError(9900, ""))
  }

  def insert(q: LtiQuiz) {
    Dao.insert(q)
  }

  def findOneById(id: ObjectId): Option[LtiQuiz] = Dao.findOneById(id)

  def findByResourceLinkId(resourceLinkId: String): Option[LtiQuiz] = {
    Dao.findOne(MongoDBObject("resourceLinkId" -> resourceLinkId))
  }

  def update(update: LtiQuiz, orgId: ObjectId): Either[ApiError, LtiQuiz] = {
    if (!canUpdate(update, orgId)) {
      Left(new ApiError(9900, "You are not allowed update the LtiQuiz - because students have already started using it."))
    } else {
      updateNoValidation(update)
    }
  }

  def canUpdate(proposedChange: LtiQuiz, orgId: ObjectId): Boolean = LtiQuiz.findOneById(proposedChange.id) match {
    case Some(dbConfig) => {
      val orgIdMatches = dbConfig.orgId.isDefined && dbConfig.orgId.get == orgId
      val isUnassigning = dbConfig.question.itemId.isDefined && proposedChange.question.itemId.isEmpty
      val settingsAreTheSame = proposedChange.question.settings.equals(dbConfig.question.settings)

      val willAffectParticipants = if (dbConfig.participants.length > 0) {
        isUnassigning || !settingsAreTheSame
      } else {
        false
      }

      orgIdMatches && !willAffectParticipants
    }
    case _ => false
  }
}
