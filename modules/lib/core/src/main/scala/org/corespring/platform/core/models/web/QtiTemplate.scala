package org.corespring.platform.core.models.web

import com.mongodb.casbah.Imports._
import com.novus.salat.dao._
import org.corespring.platform.core.models.mongoContext.context
import play.api.Play.current
import play.api.libs.json.{JsNumber, JsObject, JsString, Writes}


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
