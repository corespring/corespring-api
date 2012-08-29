package common.models

import models.ItemFile
import models.Item.JsonValidationException


import play.api.libs.json._
import play.api.libs.json.JsString

import com.novus.salat.annotations._

@Salat
abstract class SupportingMaterial(val name: String)

case class SupportingMaterialFile(override val name: String = "",
                                  file: Option[String] = None) extends SupportingMaterial(name)

case class SupportingMaterialHtml(override val name: String = "",
                                  html: String = "") extends SupportingMaterial(name)

object SupportingMaterial {

  val file: String = "file"
  val name: String = "name"
  val html: String = "html"

  implicit object SupportingMaterialWrites extends Writes[SupportingMaterial] {
    def writes(obj: SupportingMaterial) = {

      var iseq: Seq[(String, JsValue)] = Seq()
      iseq = iseq :+ (name -> JsString(obj.name))

      obj match {
        case fileMaterial: SupportingMaterialFile => {
          iseq = iseq :+ (file -> JsString(fileMaterial.file.getOrElse("")))
        }
        case htmlMaterial: SupportingMaterialHtml => {
          iseq = iseq :+ (html -> JsString(htmlMaterial.html))
        }
      }
      JsObject(iseq)
    }
  }

  implicit object SupportingMaterialReads extends Reads[SupportingMaterial] {
    def reads(json: JsValue): SupportingMaterial = {

      (json \ file).asOpt[String] match {
        case Some(foundFile) => {
          SupportingMaterialFile((json \ name).as[String], Some(foundFile))
        }
        case _ => {
          val maybeHtml: Option[String] = (json \ html).asOpt[String]
          maybeHtml match {
            case Some(foundHtml) => {
              SupportingMaterialHtml((json \ name).as[String], (json \ html).as[String])
            }
            case _ => throw new JsonValidationException("no html or file property")
          }
        }
      }
    }
  }

}

