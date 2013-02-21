package basiclti.models

import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import org.bson.types.ObjectId

import play.api.Play.current
import org.bson.types.ObjectId
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import com.novus.salat.dao._
import se.radley.plugin.salat._
import models.{mongoContext}
import mongoContext._
import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import api.ApiError
import common.models.json.jerkson.{JerksonReads, JerksonWrites}
import com.mongodb.casbah.MongoCollection
import models.itemSession.{ItemSessionSettings, ItemSession}

case class LtiQuestion(itemId: Option[ObjectId],
                       settings: ItemSessionSettings)
  extends models.quiz.BaseQuestion(itemId, settings)

case class LtiParticipant(itemSession: ObjectId,
                          resultSourcedId: String,
                          gradePassbackUrl: String,
                          onFinishedUrl: String)
  extends models.quiz.BaseParticipant(Seq(itemSession), resultSourcedId)

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
        ItemSession.save(session)

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

  implicit object Writes extends JerksonWrites[LtiQuiz]

  implicit object Reads extends JerksonReads[LtiQuiz] {
    def manifest = Manifest.classType(LtiQuiz("", LtiQuestion(null, null), Seq(), None).getClass)
  }

  /**
   * Hide the ModelCompanion from the client code as it provides too many operations.
   */
  private object Dao extends ModelCompanion[LtiQuiz, ObjectId] {
    val collection = mongoCollection("lti_quizzes")
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
      Left(new ApiError(9900, "TODO"))
    } else {
      updateNoValidation(update)
    }
  }

  def canUpdate(proposedChange: LtiQuiz, orgId: ObjectId): Boolean = LtiQuiz.findOneById(proposedChange.id) match {
    case Some(dbConfig) => {
      val orgIdMatches = dbConfig.orgId.isDefined && dbConfig.orgId.get == orgId
      val isUnassigning = dbConfig.question.itemId.isDefined && proposedChange.question.itemId.isEmpty
      val hasAssignments = dbConfig.participants.length > 0
      def canUnassign = if (isUnassigning) !hasAssignments else true
      orgIdMatches && canUnassign
    }
    case _ => false
  }
}


/**
 * A configuration object for launching a corespring item.
 * Contains an item session template and an item id
 * @param resourceLinkId
 */
case class LtiLaunchConfiguration(resourceLinkId: String,
                                  itemId: Option[ObjectId],
                                  sessionSettings: Option[ItemSessionSettings],
                                  orgId: Option[ObjectId],
                                  assignments: Seq[Assignment] = Seq(),
                                  id: ObjectId = new ObjectId()) {

  /**
   * Add an assignment if its new
   * @param resultSourcedId - the uid of the assignment
   * @param passbackUrl
   * @param finishedUrl
   * @return
   */
  def addAssignmentIfNew(resultSourcedId: String, passbackUrl: String, finishedUrl: String): LtiLaunchConfiguration = {
    assignments.find(_.resultSourcedId == resultSourcedId) match {
      case Some(a) => this
      case _ => {

        require(itemId.isDefined, "No itemId is defined")

        val session = new ItemSession(
          itemId = itemId.get,
          settings = sessionSettings.getOrElse(LtiLaunchConfiguration.defaultSessionSettings))
        ItemSession.save(session)

        val newAssignment = new Assignment(
          itemSessionId = session.id,
          resultSourcedId = resultSourcedId,
          gradePassbackUrl = passbackUrl,
          onFinishedUrl = finishedUrl
        )
        val newAssignments = this.assignments :+ newAssignment

        val newConfig = this.copy(assignments = newAssignments)

        LtiLaunchConfiguration.updateNoValidation(newConfig) match {
          case Left(e) => throw new RuntimeException("Error updating launch config: " + newConfig.id)
          case Right(updated) => updated
        }
      }
    }
  }

  def hasAssignments = this.assignments.length > 0
}

private object LtiLaunchConfiguration {

  val defaultSessionSettings = ItemSessionSettings(
    maxNoOfAttempts = 1,
    showFeedback = true,
    highlightCorrectResponse = true,
    highlightUserResponse = true,
    allowEmptyResponses = true
  )

  implicit object Writes extends JerksonWrites[LtiLaunchConfiguration]

  implicit object Reads extends JerksonReads[LtiLaunchConfiguration] {
    def manifest = Manifest.classType(new LtiLaunchConfiguration("", None, None, None).getClass)
  }

  /**
   * Hide the ModelCompanion from the client code as it provides too many operations.
   */
  private object ModelCompanion extends ModelCompanion[LtiLaunchConfiguration, ObjectId] {
    val collection = mongoCollection("lti_launch_configurations")
    collection.ensureIndex(Keys.resourceLinkId)
    val dao = new SalatDAO[LtiLaunchConfiguration, ObjectId](collection = collection) {}
  }

  object Keys {
    val resourceLinkId: String = "resourceLinkId"
    val orgId: String = "orgId"
    val sessionSettings: String = "sessionSettings"
    val itemId: String = "itemId"
    val assignments: String = "assignments"
    val id: String = "id"
  }

  def collection: MongoCollection = ModelCompanion.collection

  def findOne(q: DBObject) = ModelCompanion.findOne(q)

  def findOneById(id: ObjectId) = ModelCompanion.findOneById(id)

  def findByResourceLinkId(linkId: String): Option[LtiLaunchConfiguration] = {
    findOne(MongoDBObject(Keys.resourceLinkId -> linkId))
  }

  def canUpdate(proposedChange: LtiLaunchConfiguration, orgId: ObjectId): Boolean = LtiLaunchConfiguration.findOneById(proposedChange.id) match {
    case Some(dbConfig) => {
      val orgIdMatches = dbConfig.orgId.isDefined && dbConfig.orgId.get == orgId
      val isUnassigning = dbConfig.itemId.isDefined && proposedChange.itemId.isEmpty
      val hasAssignments = dbConfig.assignments.length > 0
      def canUnassign = if (isUnassigning) !hasAssignments else true
      orgIdMatches && canUnassign
    }
    case _ => false
  }

  def insert(c: LtiLaunchConfiguration) {
    ModelCompanion.insert(c)
  }

  def update(update: LtiLaunchConfiguration, orgId: ObjectId): Either[ApiError, LtiLaunchConfiguration] = {

    if (!canUpdate(update, orgId)) {
      Left(new ApiError(9900, "TODO"))
    } else {
      updateNoValidation(update)
    }
  }

  private def updateNoValidation(update: LtiLaunchConfiguration): Either[ApiError, LtiLaunchConfiguration] = ModelCompanion.findOneById(update.id) match {
    case Some(c) => {
      ModelCompanion.save(update)
      Right(update)
    }
    case _ => Left(new ApiError(9900, ""))
  }

}


