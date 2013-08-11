package models

import org.bson.types.ObjectId
import com.mongodb.casbah.commons._
import com.novus.salat.dao._
import se.radley.plugin.salat._
import com.novus.salat._
import com.novus.salat.global._
import play.api.Play.current

case class SchemaMetadata(key: String)
case class MetadataSet(var metadataKey: String,
                        var editorUrl: String,
                        var editorLabel: String,
                        var isPublic:Boolean = String,
                        var schema:Seq[SchemaMetadata] = Seq(),
                        var id: ObjectId = ObjectId.get())
object MetadataSet extends ModelCompanion[MetadataSet,ObjectId]{
  val collection = mongoCollection("MetadataSet")
  def dao: DAO[MetadataSet, ObjectId] = new SalatDAO[MetadataSet, ObjectId](collection = collection) {}

  def findByKey(key:String):Option[MetadataSet] = {
    findOne(MongoDBObject("metadataKey" -> key))
  }
}
