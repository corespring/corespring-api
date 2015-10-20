package org.corespring.models.metadata

import org.bson.types.ObjectId

case class SchemaMetadata(key: String)

case class MetadataSet(metadataKey: String,
  editorUrl: String,
  editorLabel: String,
  isPublic: Boolean = MetadataSet.Defaults.isPublic,
  schema: Seq[SchemaMetadata] = MetadataSet.Defaults.schema,
  id: ObjectId = MetadataSet.Defaults.id)

case class Metadata(key: String, properties: Map[String, String])

object MetadataSet {
  object Defaults {
    val isPublic = false
    val schema = Seq()
    def id = new ObjectId()
  }
}
