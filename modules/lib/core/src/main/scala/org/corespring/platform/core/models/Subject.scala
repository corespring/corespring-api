package org.corespring.platform.core.models

import com.novus.salat.dao.ModelCompanion
import play.api.Play.current
import org.bson.types.ObjectId
import play.api.libs.json._
import play.api.libs.json.JsString
import com.novus.salat.dao.{ SalatDAO, ModelCompanion }
import com.novus.salat.dao._
import se.radley.plugin.salat._
import org.corespring.platform.core.models.search.Searchable

case class Subject(var subject: Option[String] = None,
  var category: Option[String] = None,
  var id: ObjectId = new ObjectId())

object Subject extends ModelCompanion[Subject, ObjectId] with Searchable {

  val collection = mongoCollection("subjects")
  import org.corespring.platform.core.models.mongoContext.context
  val dao = new SalatDAO[Subject, ObjectId](collection = collection) {}

  val Id = "id"
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

  val description = "Subjects"

  override val searchableFields = Seq(
    Subject,
    Category)
}
