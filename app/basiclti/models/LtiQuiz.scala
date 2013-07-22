package basiclti.models


import api.ApiError
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao._
import models.itemSession.{DefaultItemSession, ItemSessionSettings, ItemSession}
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.Play.current
import play.api.libs.json._
import scala.Left
import scala.Right
import scala.Some
import se.radley.plugin.salat._

case class LtiQuestion(itemId: Option[VersionedId[ObjectId]],
                       settings: ItemSessionSettings)
  extends models.quiz.BaseQuestion(itemId, settings)


object LtiQuestion {

  implicit object Writes extends Writes[LtiQuestion] {
    import models.versioning.VersionedIdImplicits.{Writes => IdWrites}
    def writes(q: LtiQuestion): JsValue = {
      val out = Seq("settings" -> Json.toJson(q.settings)) ++ q.itemId.map( id => "itemId" -> Json.toJson(id)(IdWrites) )
      JsObject(out)
    }
  }

  import models.versioning.VersionedIdImplicits.{Reads, Writes}
  implicit val LtiQuestionReads = Json.reads[LtiQuestion]
  /*implicit object Reads extends Reads[LtiQuestion] {

    import models.versioning.VersionedIdImplicits.{Reads => IdReads}

    def reads(json: JsValue): LtiQuestion = LtiQuestion(
      (json \ "itemId").asOpt[VersionedId[ObjectId]](IdReads),
      (json \ "settings").as[ItemSessionSettings]
    )

  }*/

}

case class LtiParticipant(itemSession: ObjectId,
                          resultSourcedId: String,
                          gradePassbackUrl: String,
                          onFinishedUrl: String)
  extends models.quiz.BaseParticipant(Seq(itemSession), resultSourcedId)

object LtiParticipant{
  implicit val idReads = models.versioning.VersionedIdImplicits.Reads
  implicit val idWrites = models.versioning.VersionedIdImplicits.Writes
  implicit val Writes = Json.writes[LtiParticipant]
  implicit val Reads = Json.reads[LtiParticipant]
  /*implicit object Writes extends Writes[LtiParticipant] {

    def writes(p:LtiParticipant) : JsValue = {
      JsObject(Seq(
        "itemSession" -> JsString(p.itemSession.toString),
        "resultSourcedId" -> JsString(p.resultSourcedId),
       "gradePassbackUrl" -> JsString(p.gradePassbackUrl),
      "onFinishedUrl" -> JsString(p.onFinishedUrl)
      ))
    }
  }*/

  /*implicit object Reads extends Reads[LtiParticipant] {

    def reads( json : JsValue ) : LtiParticipant = {
      LtiParticipant(
        new ObjectId((json \ "itemSession").as[String]),
        (json \ "resultSourcedId").as[String],
        (json \ "gradePassbackUrl").as[String],
        (json \ "onFinishedUrl").as[String]
      )
    }
  }*/
}

case class LtiQuiz(resourceLinkId: String,
                   question: LtiQuestion,
                   participants: Seq[LtiParticipant] = Seq(),
                   orgId: Option[ObjectId],
                   id: ObjectId = new ObjectId())
  extends models.quiz.BaseQuiz(Seq(question), participants, id) {

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
          onFinishedUrl = finishedUrl
        )
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

  implicit object Writes extends Writes[LtiQuiz] {

    def writes(quiz: LtiQuiz): JsValue = {

      JsObject(Seq(
        "resourceLinkId" -> JsString(quiz.resourceLinkId),
        "question" -> Json.toJson(quiz.question),
        "participants" -> JsArray(quiz.participants.map(p => Json.toJson(p))),
        "id" -> JsString(quiz.id.toString)
      ) ++ quiz.orgId.map( o => "orgId" -> JsString(o.toString)))
    }
  }

  implicit object Reads extends Reads[LtiQuiz] {

    def reads(json:JsValue) : LtiQuiz = {
      LtiQuiz(
        (json \ "resourceLinkId").as[String],
        (json \ "question").as[LtiQuestion],
        (json \ "participants").as[Seq[LtiParticipant]],
        (json \ "orgId" ).asOpt[String].map( s => new ObjectId(s)),
        new ObjectId((json \ "id").as[String])
      )
    }
  }

  /**
   * Hide the ModelCompanion from the client code as it provides too many operations.
   */
  private object Dao extends ModelCompanion[LtiQuiz, ObjectId] {
    val collection = mongoCollection("lti_quizzes")
    import models.mongoContext.context
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
