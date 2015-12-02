package org.corespring.models.json

import org.bson.types.ObjectId
import org.corespring.models.Subject
import org.corespring.models.item.Subjects
import play.api.libs.json._

object SubjectsFormat {
  object Keys {
    val primarySubject = "primarySubject"
    val relatedSubject = "relatedSubject"
    val primary = "primary"
    val related = "related"
  }
}

trait SubjectsFormat extends ValueGetter with Format[Subjects] {

  import SubjectsFormat.Keys._

  implicit def sf: Writes[Subject]

  def findOneById(id: ObjectId): Option[Subject]

  def writes(s: Subjects): JsValue = {

    /**
     * Look up the subject
     *
     * @param id
     * @return
     */
    def getSubject(id: Option[ObjectId]): Option[JsValue] = id match {
      case Some(foundId) => {
        findOneById(foundId) match {
          case Some(subj) => Some(Json.toJson(subj))
          case _ => throw new RuntimeException("Can't find subject with id: " + foundId)
        }
      }
      case _ => None
    }

    def getSubjects(ids: Seq[ObjectId]): Seq[Option[JsValue]] = ids.map { oid => getSubject(Some(oid)) }

    val foundSubjects: Seq[Option[(String, JsValue)]] = Seq(
      getSubject(s.primary).map((primarySubject -> Json.toJson(_))),
      Some(relatedSubject -> Json.toJson(getSubjects(s.related))))

    JsObject(foundSubjects.flatten)
  }

  def reads(json: JsValue): JsResult[Subjects] = {
    try {
      val primarySubjectObjectId = (json \ primarySubject).asOpt[String].map(new ObjectId(_))
      val relatedSubjectObjectIds = (json \ relatedSubject).asOpt[Seq[String]].getOrElse(Seq()).map(new ObjectId(_))
      val subject = Subjects(primarySubjectObjectId, relatedSubjectObjectIds)
      JsSuccess(subject)
    } catch {
      case e: IllegalArgumentException => JsError("error parsing subjects")
    }
  }

}
