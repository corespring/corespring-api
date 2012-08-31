package models

import com.novus.salat.dao.ModelCompanion
import play.api.Play.current
import org.bson.types.ObjectId
import play.api.libs.json._
import play.api.libs.json.JsString
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import com.novus.salat.dao._
import se.radley.plugin.salat._
import mongoContext._
import controllers.QueryParser

case class Subject(subject: Option[String] = None,
                   category: Option[String] = None,
                   id: ObjectId = new ObjectId())

object Subject extends ModelCompanion[Subject, ObjectId] {

  val collection = mongoCollection("subject")
  val dao = new SalatDAO[Subject, ObjectId](collection = collection) {}

  val Id = "Id"
  val Subject = "subject"
  val Category = "category"

  implicit object SubjectWrites extends Writes[Subject] {
    def writes(subject: Subject) = {
      var iseq: Seq[(String, JsValue)] = Seq("id" -> JsString(subject.id.toString))
      subject.subject.foreach(v => iseq = iseq :+ (Subject -> JsString(v)))
      subject.category.foreach(v => iseq = iseq :+ (Category -> JsString(v)))
      JsObject(iseq)
    }
  }

  val queryFields:Seq[QueryField[Subject]] = Seq(
    QueryField(Subject, QueryField.StringType, _.subject),
    QueryField(Category, QueryField.StringType, _.category),
    QueryField(Id, QueryField.ObjectIdType, _.id)
  )

  val description = "Subjects"
}
