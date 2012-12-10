package basiclti.models

import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import org.bson.types.ObjectId

import play.api.Play.current
import org.bson.types.ObjectId
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import com.novus.salat.dao._
import se.radley.plugin.salat._
import models.{ItemSessionSettings, ItemSession, mongoContext}
import mongoContext._
import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import api.ApiError
import play.api.libs.json.Writes
import common.models.json.jerkson.{JerksonReads, JerksonWrites}


/**
 * A configuration object for launching a corespring item.
 * Contains an item session template and an item id
 * @param resourceLinkId
 */
case class LtiLaunchConfiguration(resourceLinkId:String,
                                  itemId:Option[ObjectId],
                                  sessionSettings:Option[ItemSessionSettings],
                                  id:ObjectId = new ObjectId())

object LtiLaunchConfiguration {

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

  def findOne(q:DBObject) = ModelCompanion.findOne(q)

  def findOneById(id:ObjectId) = ModelCompanion.findOneById(id)

  def findByResourceLinkId(linkId:String) : Option[LtiLaunchConfiguration] = {
    findOne(MongoDBObject(Keys.resourceLinkId -> linkId))
  }

  def save(c:LtiLaunchConfiguration) {
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

