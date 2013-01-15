package models.item

import com.mongodb.casbah.Imports._
import play.api.libs.json.{JsValue, Json}
import models.Subject
import controllers.JsonValidationException

case class Subjects(var primary: Option[ObjectId] = None,
                    var related: Option[ObjectId] = None)

object Subjects  extends ValueGetter{

  object Keys {
    val primarySubject = "primarySubject"
    val relatedSubject = "relatedSubject"
  }

  def json(s: Subjects): Seq[(String, JsValue)] = {

    import Keys._

    def getSubject(id: Option[ObjectId]): Option[JsValue] = id match {
      case Some(foundId) => {
        Subject.findOneById(foundId) match {
          case Some(subj) => Some(Json.toJson(subj))
          case _ => throw new RuntimeException("Can't find subject with id: " + foundId)
        }
      }
      case _ => None
    }

    val seqsubjects: Seq[Option[(String, JsValue)]] = Seq(
      getSubject(s.primary).map {
        found => (primarySubject -> Json.toJson(found))
      },
      getSubject(s.related).map {
        found => (relatedSubject -> Json.toJson(found))
      }
    )

    seqsubjects.flatten
  }

  def obj(json: JsValue): Option[Subjects] = {

    import Keys._

    def buildSubjectFromSeq(s: Seq[Option[String]]) = {
      if (s.isEmpty)
        None
      else
        Some(Subjects(s(0).map(new ObjectId(_)), s(1).map(new ObjectId(_))))
    }

    try {
      get[Subjects](json, Seq(primarySubject, relatedSubject), buildSubjectFromSeq)
    }
    catch {
      case e: IllegalArgumentException => throw new JsonValidationException(e.getMessage)
    }
  }
}