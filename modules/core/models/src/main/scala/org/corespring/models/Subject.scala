package org.corespring.models

import org.bson.types.ObjectId

case class Subject(
  subject: String,
  category: Option[String] = None,
  id: ObjectId = new ObjectId())

object Subject {
  val description = "Subjects"
  object Keys {
    val Id = "id"
    val Subject = "subject"
    val Category = "category"
  }
}

