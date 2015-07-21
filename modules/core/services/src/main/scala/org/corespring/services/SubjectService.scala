package org.corespring.services

import org.bson.types.ObjectId
import org.corespring.models.Subject

trait SubjectService {

  def findOneById(id: ObjectId): Option[Subject]
}
