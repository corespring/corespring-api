package basiclti.models

import api.ApiError
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao._
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.Play.current
import play.api.libs.json._
import scala.Left
import scala.Right
import scala.Some
import se.radley.plugin.salat._
import org.corespring.platform.core.models.itemSession.{ ItemSessionSettings, DefaultItemSession, ItemSession }
import org.corespring.platform.core.models.assessment.{ BaseAssessment, BaseQuestion, BaseParticipant }
import org.corespring.platform.core.models.versioning.VersionedIdImplicits

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

  import VersionedIdImplicits.{ Reads, Writes }
  implicit val LtiQuestionReads = Json.reads[LtiQuestion]
}

case class LtiParticipant(itemSession: ObjectId,
  resultSourcedId: String,
  gradePassbackUrl: String,
  onFinishedUrl: String)
  extends BaseParticipant(Seq(itemSession), resultSourcedId)

object LtiParticipant {
  import org.corespring.platform.core.models.json.{ ObjectIdReads, ObjectIdWrites }
  implicit val Writes = Json.writes[LtiParticipant]
  implicit val Reads = Json.reads[LtiParticipant]
}

case class LtiAssessment(resourceLinkId: String,
  question: LtiQuestion,
  participants: Seq[LtiParticipant] = Seq(),
  orgId: Option[ObjectId],
  id: ObjectId = new ObjectId())
  extends BaseAssessment(Seq(question), participants, id) {

  def hasAssignments = this.participants.length > 0

  def addParticipantIfNew(resultSourcedId: String,
    passbackUrl: String,
    finishedUrl: String): LtiAssessment = {

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

        val updatedAssessment = this.copy(participants = updated)

        LtiAssessment.updateNoValidation(updatedAssessment) match {
          case Left(e) => throw new RuntimeException("Error updating LtiAssessments")
          case Right(q) => q
        }
      }
    }
  }
}

object LtiAssessment {

  /*
  implicit object Writes extends Writes[LtiAssessment] {


    def writes(assessment: LtiAssessment): JsValue = {

      JsObject(Seq(
        "resourceLinkId" -> JsString(assessment.resourceLinkId),
        "question" -> Json.toJson(assessment.question),
        "participants" -> JsArray(assessment.participants.map(p => Json.toJson(p))),
        "id" -> JsString(assessment.id.toString)
      ) ++ assessment.orgId.map( o => "orgId" -> JsString(o.toString)))
    }
  }
*/
  import org.corespring.platform.core.models.json._
  implicit val Writes = Json.writes[LtiAssessment]
  implicit val Reads = Json.reads[LtiAssessment]

  /*  implicit object Reads extends Reads[LtiAssessment] {

    def reads(json:JsValue) : LtiAssessment = {
      LtiAssessment(
        (json \ "resourceLinkId").as[String],
        (json \ "question").as[LtiQuestion],
        (json \ "participants").as[Seq[LtiParticipant]],
        (json \ "orgId" ).asOpt[String].map( s => new ObjectId(s)),
        new ObjectId((json \ "id").as[String])
      )
    }
  }
  */

  /**
   * Hide the ModelCompanion from the client code as it provides too many operations.
   */
  private object Dao extends ModelCompanion[LtiAssessment, ObjectId] {
    val collection = mongoCollection("lti_assessments")
    import org.corespring.platform.core.models.mongoContext.context
    val dao = new SalatDAO[LtiAssessment, ObjectId](collection = collection) {}
  }

  def collection = Dao.collection

  private def updateNoValidation(update: LtiAssessment): Either[ApiError, LtiAssessment] = Dao.findOneById(update.id) match {
    case Some(c) => {
      Dao.save(update)
      Right(update)
    }
    case _ => Left(new ApiError(9900, ""))
  }

  def insert(q: LtiAssessment) {
    Dao.insert(q)
  }

  def findOneById(id: ObjectId): Option[LtiAssessment] = Dao.findOneById(id)

  def findByResourceLinkId(resourceLinkId: String): Option[LtiAssessment] = {
    Dao.findOne(MongoDBObject("resourceLinkId" -> resourceLinkId))
  }

  def update(update: LtiAssessment, orgId: ObjectId): Either[ApiError, LtiAssessment] = {
    if (!canUpdate(update, orgId)) {
      Left(new ApiError(9900, "You are not allowed update the LtiAssessment - because students have already started using it."))
    } else {
      updateNoValidation(update)
    }
  }

  def canUpdate(proposedChange: LtiAssessment, orgId: ObjectId): Boolean = LtiAssessment.findOneById(proposedChange.id) match {
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
