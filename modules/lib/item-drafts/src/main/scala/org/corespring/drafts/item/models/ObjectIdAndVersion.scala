package org.corespring.drafts.item.models

import org.bson.types.ObjectId
import org.corespring.drafts.IdAndVersion

case class ObjectIdAndVersion(id: ObjectId, version: Long) extends IdAndVersion[ObjectId, Long]
