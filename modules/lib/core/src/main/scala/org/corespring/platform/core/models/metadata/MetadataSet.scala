package org.corespring.platform.core.models.metadata

import com.mongodb.casbah.commons._
import com.novus.salat.dao._
import com.novus.salat.global._
import org.bson.types.ObjectId
import play.api.Play.current
import play.api.libs.json._
import se.radley.plugin.salat._

case class SchemaMetadata(key: String)

object SchemaMetadata {
  implicit val format = Json.format[SchemaMetadata]
}

case class MetadataSet(metadataKey: String,
  editorUrl: String,
  editorLabel: String,
  isPublic: Boolean = MetadataSet.Defaults.isPublic,
  schema: Seq[SchemaMetadata] = MetadataSet.Defaults.schema,
  id: ObjectId = MetadataSet.Defaults.id)

object MetadataSet {

  object Defaults {
    val isPublic = false
    val schema = Seq()
    def id = new ObjectId()
  }

  import org.corespring.models.json._

  implicit val format = new Format[MetadataSet] {

    override def writes(metadataSet: MetadataSet): JsValue = Json.writes[MetadataSet].writes(metadataSet)
    override def reads(json: JsValue): JsResult[MetadataSet] = JsSuccess(MetadataSet(
      metadataKey = (json \ "metadataKey").as[String],
      editorUrl = (json \ "editorUrl").as[String],
      editorLabel = (json \ "editorLabel").as[String],
      isPublic = (json \ "isPublic").asOpt[Boolean].getOrElse(MetadataSet.Defaults.isPublic),
      schema = (json \ "schema").asOpt[Seq[SchemaMetadata]].getOrElse(MetadataSet.Defaults.schema),
      id = (json \ "id").asOpt[String].map(new ObjectId(_)).getOrElse(MetadataSet.Defaults.id)))
  }

}

case class Metadata(key: String, properties: Map[String, String])
