package org.corespring.models.json

import org.bson.types.ObjectId
import org.corespring.models.Subject
import org.corespring.models.item.{ Subjects, FieldValue }
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json._

class SubjectsFormatTest extends Specification {

  class scope(val primary: Option[Subject] = None, val related: Option[Subject] = None) extends Scope {

    val format = new SubjectsFormat {
      override def findOneById(id: ObjectId): Option[Subject] = if (primary.isDefined && primary.get.id == id) {
        primary
      } else if (related.isDefined && related.get.id == id) {
        related
      } else None

      override implicit def sf: Writes[Subject] = SubjectWrites

      override def fieldValues: FieldValue = FieldValue()
    }

  }

  import SubjectsFormat.Keys

  "subjects format" should {
    "read json" in new scope {

      val oid = ObjectId.get

      val json = Json.obj(
        Keys.primarySubject -> oid.toString,
        Keys.relatedSubject -> Json.arr(oid.toString)
      )

      format.reads(json) match {
        case JsSuccess(subjects, _) => {
          subjects.primary === Some(oid)
          subjects.related === Seq(oid)
        }
        case JsError(_) => failure("error reading json")
      }
    }

    "write throws an exception if it can't find the subject" in new scope {
      val subjects = Subjects(
        primary = Some(ObjectId.get),
        related = Seq(ObjectId.get)
      )

      format.writes(subjects) must throwA[RuntimeException]
    }

    "write subjects to json" in new scope(
      primary = Some(Subject(id = ObjectId.get, subject = "primary")),
      related = Some(Subject(id = ObjectId.get, subject = "related"))
    ) {
      val subjects = Subjects(
        primary = Some(primary.get.id),
        related = Seq(related.get.id)
      )

      val json = format.writes(subjects)

      (json \ Keys.primarySubject \ "subject").asOpt[String] === Some("primary")
      (json \ Keys.relatedSubject).as[Seq[JsObject]].map( j => (j \ "subject").as[String] ) === Seq("related")
    }
  }
}
