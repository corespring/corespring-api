package org.corespring.services

import com.mongodb.DBObject
import org.bson.types.ObjectId
import org.corespring.models.Subject

trait SubjectService extends QueryService[Subject] {

  def count(query: DBObject): Long

  def findOneById(id: ObjectId): Option[Subject]
}
