package org.corespring.models.json

import org.corespring.models.Subject
import play.api.libs.json._

object SubjectWrites extends Writes[Subject] {
  val Id = "id"
  val Subject = "subject"
  val Category = "category"

  def writes(subject: Subject) = {
    Json.obj("id" -> subject.id.toString,
      "subject" -> subject.subject,
      "category" -> subject.category)
  }
}

