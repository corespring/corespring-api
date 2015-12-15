package org.corespring.models.json

import org.corespring.models.Standard
import play.api.libs.json._

object StandardFormat extends Format[Standard] with JsonUtil {

  object Keys {
    val Id = "id"
    val DotNotation = "dotNotation"
    val Subject = "subject"
    val Category = "category"
    val SubCategory = "subCategory"
    val Standard = "standard"
    val Domain = "domain"
    val guid = "guid"
    val grades = "grades"
  }

  def writes(obj: Standard) = {

    partialObj(
      Keys.Category -> Some(JsString(obj.category.getOrElse(""))),
      Keys.Domain -> Some(JsString(obj.domain.getOrElse(""))),
      Keys.DotNotation -> Some(JsString(obj.dotNotation.getOrElse(""))),
      Keys.Id -> Some(JsString(obj.id.toString)),
      Keys.Standard -> Some(JsString(obj.standard.getOrElse(""))),
      Keys.SubCategory -> Some(JsString(obj.subCategory.getOrElse(""))),
      Keys.Subject -> Some(JsString(obj.subject.getOrElse(""))),
      Keys.grades -> (obj.grades match {
        case nonEmpty if Keys.grades.nonEmpty => Some(JsArray(obj.grades.map(JsString(_))))
        case _ => None
      }))
  }

  def reads(json: JsValue) = {

    val standard = Standard(
      dotNotation = (json \ Keys.DotNotation).asOpt[String],
      guid = (json \ Keys.guid).asOpt[String],
      subject = (json \ Keys.Subject).asOpt[String],
      category = (json \ Keys.Category).asOpt[String],
      subCategory = (json \ Keys.SubCategory).asOpt[String],
      standard = (json \ Keys.Standard).asOpt[String],
      grades = (json \ Keys.grades).as[Seq[String]])

    JsSuccess(standard)
  }

}
