package org.corespring.services.metadata

import org.bson.types.ObjectId
import org.corespring.models.metadata.Metadata
import org.corespring.platform.data.mongo.models.VersionedId

trait MetadataService {

  def get(itemId: VersionedId[ObjectId], keys: Seq[String]): Seq[Metadata]
}
