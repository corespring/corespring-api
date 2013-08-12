package models

import org.bson.types.ObjectId
import com.mongodb.casbah.commons._
import com.novus.salat.dao._
import se.radley.plugin.salat._
import com.novus.salat._
import com.novus.salat.global._
import play.api.Play.current
import scalaz._
import Scalaz._

case class SchemaMetadata(key: String)
case class MetadataSet(var metadataKey: String,
                        var editorUrl: String,
                        var editorLabel: String,
                        var isPublic:Boolean = false,
                        var schema:Seq[SchemaMetadata] = Seq(),
                        var id: ObjectId = ObjectId.get())
object MetadataSet extends ModelCompanion[MetadataSet,ObjectId]{
  val collection = mongoCollection("metadataSet")
  def dao: DAO[MetadataSet, ObjectId] = new SalatDAO[MetadataSet, ObjectId](collection = collection) {}

  def findByKey(key:String):Option[MetadataSet] = {
    findOne(MongoDBObject("metadataKey" -> key))
  }

  def insertMetadata(ms: MetadataSet, orgId:ObjectId):ValidationNel[controllers.InternalError,MetadataSet] = {
    try{
      insert(ms.copy(id = ObjectId.get())) match {
        case Some(id) => {
          Organization.addMetadataSet(orgId, id, false).fold(Failure(_), _ => ms.copy(id = id).successNel[controllers.InternalError])
        }
        case None => controllers.InternalError("error while inserting metadata set").failNel[MetadataSet]
      }
    } catch {
      case e:SalatInsertError => controllers.InternalError("error while inserting metadata set", e).failNel[MetadataSet]
    }
  }
}
