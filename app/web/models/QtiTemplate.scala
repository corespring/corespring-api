package web.models

import com.novus.salat._
import play.api.Play.current

import com.novus.salat.global._
import com.novus.salat.dao._
import play.api.libs.json.{JsObject, JsString, Writes}

import com.mongodb.casbah.Imports._

case class QtiTemplate(
                        _id: ObjectId = new ObjectId,
                        label: String,
                        code: String,
                        group: String,
                        xmlData: String
                        )


object QtiTemplate extends ModelCompanion[QtiTemplate, ObjectId] {
  val collection = se.radley.plugin.salat.mongoCollection("templates")
  val dao = new SalatDAO[QtiTemplate, ObjectId](collection = collection) {}


  implicit object QtiTemplateWrites extends Writes[QtiTemplate] {
    def writes(template: QtiTemplate) = {

      JsObject(
        Seq(
          "id" -> JsString(template._id.toString()),
          "label" -> JsString(template.label),
          "code" -> JsString(template.code),
          "group" -> JsString(template.group),
          "xmlData" -> JsString(template.xmlData)
        )
      )

    }
  }

}
