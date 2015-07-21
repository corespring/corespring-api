package org.corespring.models.json.metadata

import org.bson.types.ObjectId
import org.corespring.models.json.ObjectIdFormat
import org.corespring.models.metadata.{ SchemaMetadata, MetadataSet }
import play.api.libs.json._

object MetadataSetFormat extends Format[MetadataSet] {

  implicit val objectId: Format[ObjectId] = ObjectIdFormat
  implicit val schemaMetadata: Format[SchemaMetadata] = Json.format[SchemaMetadata]

  override def writes(metadataSet: MetadataSet): JsValue = {
    Json.writes[MetadataSet].writes(metadataSet)
  }
  override def reads(json: JsValue): JsResult[MetadataSet] = JsSuccess(MetadataSet(
    metadataKey = (json \ "metadataKey").as[String],
    editorUrl = (json \ "editorUrl").as[String],
    editorLabel = (json \ "editorLabel").as[String],
    isPublic = (json \ "isPublic").asOpt[Boolean].getOrElse(MetadataSet.Defaults.isPublic),
    schema = (json \ "schema").asOpt[Seq[SchemaMetadata]].getOrElse(MetadataSet.Defaults.schema),
    id = (json \ "id").asOpt[String].map(new ObjectId(_)).getOrElse(MetadataSet.Defaults.id)))
}

