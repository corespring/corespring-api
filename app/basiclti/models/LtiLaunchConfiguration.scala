package basiclti.models

import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import org.bson.types.ObjectId

import play.api.Play.current
import org.bson.types.ObjectId
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import com.novus.salat.dao._
import se.radley.plugin.salat._
import models.{ItemSession, ItemSessionSettings, mongoContext}
import mongoContext._
import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import api.ApiError
import common.models.json.jerkson.{JerksonReads, JerksonWrites}
import com.mongodb.casbah.MongoCollection


/**
 * A configuration object for launching a corespring item.
 * Contains an item session template and an item id
 * @param resourceLinkId
 */
case class LtiLaunchConfiguration(resourceLinkId:String,
                                  itemId:Option[ObjectId],
                                  sessionSettings:Option[ItemSessionSettings],
                                  assignments : Seq[Assignment] = Seq(),
                                  id:ObjectId = new ObjectId())
{



  def addAssignment(resultSourcedId:String, passbackUrl:String, finishedUrl:String) : LtiLaunchConfiguration = {
    assignments.find( _.resultSourcedId == resultSourcedId) match {
      case Some(a) => this
      case _ => {

        require(itemId.isDefined, "No itemId is defined")

        val session = new ItemSession(
          itemId = itemId.get,
          settings = sessionSettings.getOrElse(LtiLaunchConfiguration.defaultSessionSettings) )
        ItemSession.save(session)

        val newAssignment = new Assignment(
          itemSessionId = session.id,
          resultSourcedId = resultSourcedId,
          gradePassbackUrl = passbackUrl,
          onFinishedUrl = finishedUrl
        )
        val newAssignments = this.assignments :+ newAssignment
        val newConfig = new LtiLaunchConfiguration(
          this.resourceLinkId,
          this.itemId,
          this.sessionSettings,
          newAssignments,
          this.id)
        LtiLaunchConfiguration.update(newConfig)
        newConfig
      }
    }
  }
}

object LtiLaunchConfiguration {

  val defaultSessionSettings = ItemSessionSettings(
    maxNoOfAttempts = 1,
    showFeedback = true,
    highlightCorrectResponse = true,
    highlightUserResponse = true,
    allowEmptyResponses = true
  )

  implicit object Writes extends JerksonWrites[LtiLaunchConfiguration]

  implicit object Reads extends JerksonReads[LtiLaunchConfiguration] {
    def manifest = Manifest.classType( new LtiLaunchConfiguration("",None, None).getClass)
  }

  /**
   * Hide the ModelCompanion from the client code as it provides too many operations.
   */
  private object ModelCompanion extends ModelCompanion[LtiLaunchConfiguration,ObjectId] {
    val collection = mongoCollection("lti_launch_configurations")
    collection.ensureIndex(Keys.resourceLinkId)
    val dao = new SalatDAO[LtiLaunchConfiguration, ObjectId](collection = collection) {}
  }

  object Keys{
    val resourceLinkId:String = "resourceLinkId"
  }

  def collection : MongoCollection = ModelCompanion.collection

  def findOne(q:DBObject) = ModelCompanion.findOne(q)

  def findOneById(id:ObjectId) = ModelCompanion.findOneById(id)

  def findByResourceLinkId(linkId:String) : Option[LtiLaunchConfiguration] = {
    findOne(MongoDBObject(Keys.resourceLinkId -> linkId))
  }

  def create(c:LtiLaunchConfiguration) {
    ModelCompanion.insert(c)
  }

  def update(update : LtiLaunchConfiguration) : Either[ApiError,LtiLaunchConfiguration] = {
   ModelCompanion.findOneById(update.id) match {
     case Some(c) => {
       ModelCompanion.save(update)
       Right(update)
     }
     case _ => Left(new ApiError(9900, ""))
   }
  }


}

