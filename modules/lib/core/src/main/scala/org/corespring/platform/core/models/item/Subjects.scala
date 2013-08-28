package org.corespring.platform.core.models.item

import com.mongodb.casbah.commons.TypeImports.ObjectId
import org.corespring.platform.core.models.Subject
import org.corespring.platform.core.models.json.JsonValidationException
import play.api.libs.json._
import scala.Some

case class Subjects(var primary: Option[ObjectId] = None,
  var related: Option[ObjectId] = None)

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

      val foundSubjects: Seq[Option[(String, JsValue)]] = Seq(
        getSubject(s.primary).map((primarySubject -> Json.toJson(_))),
        getSubject(s.related).map((relatedSubject -> Json.toJson(_))))

      JsObject(foundSubjects.flatten)
    }
  }

  implicit object Reads extends Reads[Subjects] {
    def reads(json: JsValue): JsResult[Subjects] = {
      import Keys._

      def buildSubjectFromSeq(s: Seq[Option[String]]) = {
        if (s.isEmpty)
          None
        else
          Some(Subjects(s(0).map(new ObjectId(_)), s(1).map(new ObjectId(_))))
      }

      try {
        val maybeSubjects = get[Subjects](json, Seq(primarySubject, relatedSubject), buildSubjectFromSeq)
        maybeSubjects match {
          case Some(s) => JsSuccess(s)
          case _ => JsError("error parsing subjects")
        }
      } catch {
        case e: IllegalArgumentException => JsError("error parsing subjects")
      }
    }

  }
}