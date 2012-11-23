package basiclti.models

import org.bson.types.ObjectId
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import se.radley.plugin.salat._

/**
 * Connects an LTI Assignment with a corespring ItemSession
 */
case class Assignment(
                       //Uniquely identifies the student's interaction with the item session.
                       resultSourceId: Option[String] = None,
                       itemSessionId: Option[ObjectId] = None,
                       id: ObjectId = new ObjectId())


object Assignment extends ModelCompanion[Assignment, ObjectId] {
  val collection = mongoCollection("lti_assignments")
  val dao = new SalatDAO[Assignment, ObjectId](collection = collection) {}
}
