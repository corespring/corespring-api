package org.corespring.it.helpers.models

import org.corespring.models.Subject
import org.bson.types.ObjectId

object SubjectHelper {

  def create(subjects: Subject*): Seq[ObjectId] = {

    def saveSubjects(s: Subject) = {
      Subject.save(s)
      s.id
    }
    subjects.map(saveSubjects)
  }

  def delete(ids: Seq[ObjectId]) = {
    ids.foreach(Subject.removeById(_))
  }

}
