package org.corespring.services

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.models.Subject

trait SubjectService extends QueryService[Subject] {

  def count(query: MongoDBObject): Long

  def findOneById(id: ObjectId): Option[Subject]
}
