package org.corespring.services

import org.bson.types.ObjectId
import org.corespring.models.Subject

trait SubjectService extends QueryService[Subject] {

  def findOneById(id: ObjectId): Option[Subject]
}
