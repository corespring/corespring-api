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
import play.api.libs.json.Json

case class SchemaMetadata(key: String)

object SchemaMetadata{
  implicit val format = Json.format[SchemaMetadata]
}

case class MetadataSet(var metadataKey: String,
                        var editorUrl: String,
                        var editorLabel: String,
                        var isPublic:Boolean = false,
                        var schema:Seq[SchemaMetadata] = Seq(),
                        var id: ObjectId = ObjectId.get())


object MetadataSet{

  import common.models.json._

  implicit val format = Json.format[MetadataSet]

}

case class Metadata(key:String, properties:Map[String,String])