package web.controllers

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
