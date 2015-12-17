package org.corespring.models.item

import org.bson.types.ObjectId
import org.corespring.models.item.resource.BaseFile
import org.corespring.platform.data.mongo.models.{VersionedId, EntityWithVersionedId}

case class Passage(id: VersionedId[ObjectId] = VersionedId(ObjectId.get(), Some(0)),
  contentType: String = Passage.contentType,
  collectionId: String,
  file: BaseFile) extends Content[VersionedId[ObjectId]] with EntityWithVersionedId[ObjectId] {
}

object Passage {
  val contentType = "passage"
}