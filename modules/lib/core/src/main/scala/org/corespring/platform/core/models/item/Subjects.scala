package org.corespring.platform.core.models.item

import com.mongodb.casbah.commons.TypeImports.ObjectId
import org.corespring.platform.core.models.Subject
import play.api.libs.json._
import scala.Some

case class Subjects(var primary: Option[ObjectId] = None,
  var related: Seq[ObjectId] = Seq())

object Subjects extends ValueGetter {

  object Keys {
    val primarySubject = "primarySubject"
    val relatedSubject = "relatedSubject"
    val primary = "primary"
    val related = "related"
  }

  implicit object Writes extends Writes[Subjects] {
    def writes(s: Subjects): JsValue = {

      import Keys._

      /**
       * Look up the subject in the database
       *
       * @param id
       * @return
       */
      def getSubject(id: Option[ObjectId]): Option[JsValue] = id match {
        case Some(foundId) => {
          Subject.findOneById(foundId) match {
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
  }

  implicit object Reads extends Reads[Subjects] {
    def reads(json: JsValue): JsResult[Subjects] = {
      import Keys._

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
}