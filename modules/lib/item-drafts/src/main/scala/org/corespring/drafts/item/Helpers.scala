package org.corespring.drafts.item

import org.bson.types.ObjectId
import org.corespring.drafts.item.models.ObjectIdAndVersion
import org.corespring.platform.data.mongo.models.VersionedId

private[corespring] object Helpers {

  import scala.language.implicitConversions

  implicit def vidToObjectIdAndVersion(vid: VersionedId[ObjectId]): ObjectIdAndVersion = {
    vid.version.map { v =>
      ObjectIdAndVersion(vid.id, v)
    }.getOrElse { throw new IllegalArgumentException("VersionedId must have a version defined") }
  }
}

