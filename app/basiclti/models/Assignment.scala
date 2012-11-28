package basiclti.models

import play.api.Play.current
import org.bson.types.ObjectId
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import com.novus.salat.dao._
import se.radley.plugin.salat._
import models.mongoContext
import mongoContext._

/**
 * Connects an LTI Assignment with a corespring ItemSession
 */
case class Assignment(
                       //Uniquely identifies the student's interaction with the item session.
                       resultSourcedId: String,
                       itemSessionId: ObjectId,
                       gradePassbackUrl : String,
                       onFinishedUrl : String,
                       id: ObjectId = new ObjectId())


object Assignment extends ModelCompanion[Assignment, ObjectId] {
  val collection = mongoCollection("lti_assignments")
  val dao = new SalatDAO[Assignment, ObjectId](collection = collection) {}
}
