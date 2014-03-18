package org.corespring.test.helpers.models

import org.corespring.platform.core.models.Subject
import org.bson.types.ObjectId

object SubjectHelper {

  def create(subjects: String*): Seq[ObjectId] = {

    def saveSubjects(s: String) = {
      val obj = Subject(
        subject = Some(s),
        id = ObjectId.get)
      Subject.save(obj)
      obj.id
    }
    subjects.map(saveSubjects)
  }

  def delete(ids: Seq[ObjectId]) = {
    ids.foreach(Subject.removeById(_))
  }

}
