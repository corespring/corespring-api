package org.corespring.platform.core.models

import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao._
import org.bson.types.ObjectId
import org.corespring.platform.core.models.search.Searchable
import org.corespring.platform.core.services.QueryService
import play.api.Play.current
import play.api.libs.json._
import se.radley.plugin.salat._

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
