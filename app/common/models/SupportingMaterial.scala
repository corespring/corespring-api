package common.models

import models.ItemFile
import models.Item.JsonValidationException
import models.mongoContext._
import play.api.libs.json._
import play.api.libs.json.JsString

import com.novus.salat.annotations._

@Salat
abstract class SupportingMaterial(val name: String)
//TODO: Do we need to support content-types for files?
case class SupportingMaterialFile(override val name:String = "") extends SupportingMaterial(name)

case class SupportingMaterialHtml(override val name: String = "",
                                  files: Seq[InlineFile] = Seq() ) extends SupportingMaterial(name)

case class InlineFile(name:String="", content:String="")

object InlineFile{
  implicit object InlineFileWrites extends Writes[InlineFile]{
    def writes(obj: InlineFile) =  JsObject(Seq(("name" -> JsString(obj.name)), ("content"-> JsString(obj.content))))
  }

  implicit object InlineFileReads extends Reads[InlineFile]{
    def reads(json:JsValue): InlineFile =  InlineFile( (json\"name").asOpt[String].getOrElse(""), (json\"content").asOpt[String].getOrElse(""))
  }
}

object SupportingMaterial {

  val name: String = "name"
  val html: String = "html"

  implicit object SupportingMaterialWrites extends Writes[SupportingMaterial] {
    def writes(obj: SupportingMaterial) = {

      var iseq: Seq[(String, JsValue)] = Seq()
      iseq = iseq :+ (name -> JsString(obj.name))

      obj match {
        case htmlMaterial: SupportingMaterialHtml => {
          val jsonFiles = htmlMaterial.files.map( (f : InlineFile) =>  Json.toJson(f)  )
          iseq = iseq :+ ("files" -> JsArray(jsonFiles))
        }
        case file : SupportingMaterialFile => //do nothing
      }
      JsObject(iseq)
    }
  }

  implicit object SupportingMaterialReads extends Reads[SupportingMaterial] {
    def reads(json: JsValue): SupportingMaterial = {
      (json \ "files").asOpt[Seq[InlineFile]] match {
        case Some(foundFiles) => {
          SupportingMaterialHtml((json \ name).as[String], foundFiles )
        }
        case _ => SupportingMaterialFile( (json \ name ).as[String])
      }
    }
  }
}

