package org.corespring.common.mongo

import org.bson.types.ObjectId

trait ObjectIdParser {

  def objectId(id: String): Option[ObjectId] = {
    try {
      Some(new ObjectId(id))
    } catch {
      case e: Exception => None
    }
  }

}
