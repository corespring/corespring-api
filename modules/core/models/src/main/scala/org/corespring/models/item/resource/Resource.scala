package org.corespring.models.item.resource

import org.bson.types.ObjectId

/**
 * A Resource is representation of a set of one or more files. The files can be Stored files (uploaded to amazon) or virtual files (stored in mongo).
 */
case class Resource(id: Option[ObjectId] = None,
  name: String,
  materialType: Option[String] = None,
  files: Seq[BaseFile]) {
  def defaultFile = files.find(_.isMain)
}

object Resource {
  val DataPath = "data"
}

