package common.models

import models.ItemFile
import org.bson.types.ObjectId
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import se.radley.plugin.salat._
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import models.Item.JsonValidationException


import play.api.Play.current
import org.bson.types.ObjectId
import play.api.libs.json._
import play.api.libs.json.JsString
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import com.mongodb.casbah.Imports._
import controllers.{LogType, InternalError}
import collection.mutable
import scala.Either
import models.mongoContext._

//case class SupportingMaterial(materialType: String, var name : String)

case class SupportingMaterialFile(var name: String = "",
                                  var file: Option[ItemFile] = None,
                                  var id: ObjectId = new ObjectId())

//case class SupportingMaterialFileInline(materialType: String = "inline", content: String = "", files: Seq[ItemFile] = Seq()) extends SupportingMaterialFile

object SupportingMaterialFile extends ModelCompanion[SupportingMaterialFile, ObjectId] {

  val fileType: String = "file"
  val inlineType: String = "inline"
  val file : String = "file"
  val name : String = "name"

  val collection = mongoCollection("supportingMaterials")
  val dao = new SalatDAO[SupportingMaterialFile, ObjectId](collection = collection) {}

  implicit object SupportingMaterialFileWrites extends Writes[SupportingMaterialFile] {
    def writes(obj: SupportingMaterialFile) = {
      var iseq: Seq[(String, JsValue)] = Seq("id" -> JsString(obj.id.toString))
      obj.file.foreach(v => iseq = iseq :+ ("file" -> Json.toJson(v)))
      iseq = iseq :+ ("name" -> JsString(obj.name))
      JsObject(iseq)
    }
  }

  implicit object SupportingMaterialFileReads extends Reads[SupportingMaterialFile] {
    def reads(json: JsValue): SupportingMaterialFile = {
      SupportingMaterialFile(
        (json \ name).asOpt[String].getOrElse(""),
        (json \ file).asOpt[ItemFile]
      )
    }
  }

}

