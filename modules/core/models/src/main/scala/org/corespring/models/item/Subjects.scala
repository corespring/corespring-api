package org.corespring.models.item

import org.bson.types.ObjectId

case class Subjects(primary: Option[ObjectId] = None,
  related: Seq[ObjectId] = Seq.empty)

object Subjects {
  object Keys {
    val primary = "primary"
    val related = "related"
  }
}
