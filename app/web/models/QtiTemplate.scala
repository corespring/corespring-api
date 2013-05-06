package web.models

import com.novus.salat._
import play.api.Play.current

import com.novus.salat.global._
import com.novus.salat.dao._
import play.api.libs.json.{JsNumber, JsObject, JsString, Writes}

import com.mongodb.casbah.Imports._

case class QtiTemplate(
                        _id: ObjectId = new ObjectId,
                        label: String,
                        code: String,
                        group: String,
                        position: Int = 0,
                        xmlData: String
                        )


object QtiTemplate extends ModelCompanion[QtiTemplate, ObjectId] {
  //Note - use def so that we don't get Mongo connection closed errors from Salat.
  def collection = se.radley.plugin.salat.mongoCollection("templates")

  def dao = new SalatDAO[QtiTemplate, ObjectId](collection = collection) {}


  implicit object QtiTemplateWrites extends Writes[QtiTemplate] {
    def writes(template: QtiTemplate) = {


      JsObject(
        Seq(
          "id" -> JsString(template._id.toString()),
          "label" -> JsString(template.label),
          "code" -> JsString(template.code),
          "group" -> JsString(template.group),
          "position" -> JsNumber(template.position),
          "xmlData" -> JsString(template.xmlData)
        )
      )

    }
  }

}
