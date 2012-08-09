import _root_.controllers.auth.Permission
import _root_.controllers.services.OrgService
import _root_.models.{ContentCollection, Organization}
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import play.api._

/**
 */
object Global extends GlobalSettings {

  override def onStart(app: Application) {
    if ( Play.isDev(app) ) {
//      Logger.info("Loading data for development")
//      val parentId = new ObjectId("5019921244ae551130b4b28e")
//      val childId = new ObjectId("5019921244ae551130b4b28f")
//      if ( Organization.findOneById(parentId).isEmpty) {
//        val o = Organization(id = parentId, name = "Acme")
//        Organization.insert(o,None)
//      }
//
//      if ( Organization.findOneById(childId).isEmpty ) {
//        val c = Organization(id = childId, name = "Branch 1")
//        Organization.insert(c,Some(parentId))
//      }
//
//      Logger.info("Acme id = %s".format(parentId.toString))
//      Logger.info("Branch id = %s".format(childId.toString))
    }
  }
}
