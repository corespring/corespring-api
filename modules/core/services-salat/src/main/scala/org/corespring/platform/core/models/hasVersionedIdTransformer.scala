package org.corespring.platform.core.models

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedIdTransformer

trait hasVersionedIdTransformer {
  def versionedIdTransformer = new VersionedIdTransformer[ObjectId]()
}
