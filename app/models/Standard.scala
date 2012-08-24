package models

import play.api.Play.current
import org.bson.types.ObjectId
import play.api.libs.json._
import play.api.libs.json.JsString
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import com.novus.salat.dao._
import se.radley.plugin.salat._
import mongoContext._

case class Standard(
                     dotNotation: Option[String] = None,
                     subject: Option[String],
                     category: Option[String],
                     subCategory: Option[String],
                     standard: Option[String]
                     )

object Standard extends ModelCompanion[Standard, ObjectId] {

  val collection = mongoCollection("cc-standards")
  val dao = new SalatDAO[Standard, ObjectId](collection = collection) {}

  val Id = "Id"
  val DotNotation = "dotNotation"
  val Subject = "subject"
  val Category = "category"
  val SubCategory = "subCategory"
  val Standard = "standard"

  implicit object StandardWrites extends Writes[Standard] {
    def writes(obj: Standard) = {
      JsObject(
        List(
          DotNotation -> JsString(obj.dotNotation.getOrElse("")),
          Subject -> JsString(obj.subject.getOrElse("")),
          Category -> JsString(obj.category.getOrElse("")),
          SubCategory -> JsString(obj.subCategory.getOrElse("")),
          Standard -> JsString(obj.standard.getOrElse(""))
        )
      )
    }
  }

  val queryFields = Map(
    Id -> "String",
    DotNotation -> "String",
    Subject -> "String",
    Category -> "String",
    SubCategory -> "String",
    Standard -> "String"
  )

  val description = "common core state standards"
}

