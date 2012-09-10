package web.models

import com.novus.salat._
import play.api.Play.current

import com.novus.salat.global._
import com.novus.salat.dao._
import com.typesafe.config.{Config, ConfigFactory}
import play.api.Logger
import play.api.libs.json.{Reads, JsObject, JsString, Writes}

//import com.mongodb.casbah.commons.Imports._
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.MongoURI

case class QtiTemplate(
                        _id: ObjectId = new ObjectId,
                        label: String,
                        code:String,
                        xmlData: String
                        )


object QtiTemplate extends ModelCompanion[QtiTemplate, ObjectId] {
  val collection = se.radley.plugin.salat.mongoCollection("templates")
  val dao = new SalatDAO[QtiTemplate, ObjectId](collection = collection) {}


  implicit object QtiTemplateWrites extends Writes[QtiTemplate] {
    def writes(template:QtiTemplate) = {

      JsObject(
        Seq(
          "id" -> JsString(template._id.toString()),
          "label" -> JsString(template.label),
          "code" -> JsString(template.code),
          "xmlData" -> JsString(template.xmlData)
        )
      )

    }
  }

}
