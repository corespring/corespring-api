package org.corespring.models.item.resource

import org.bson.types.ObjectId

/**
 * A Resource is representation of a set of one or more files.
 * The files can be Stored files (uploaded to amazon)
 * or virtual files (stored in mongo).
 */
case class Resource(id: Option[ObjectId] = None,
  name: String,
  materialType: Option[String] = None,
  files: Seq[BaseFile]) {

  def defaultFile = files.find(_.isMain)

  def defaultVirtualFile: Option[VirtualFile] = defaultFile.flatMap {
    case vf: VirtualFile => Some(vf)
    case _ => None
  }

  def defaultStoredFile: Option[StoredFile] = defaultFile.flatMap {
    case sf: StoredFile => Some(sf)
    case _ => None
  }

  def storedFiles: Seq[StoredFile] = files.filter(_.isInstanceOf[StoredFile]).map(_.asInstanceOf[StoredFile])
  def virtualFiles: Seq[VirtualFile] = files.filter(_.isInstanceOf[VirtualFile]).map(_.asInstanceOf[VirtualFile])
}

object Resource {
  val DataPath = "data"
}

