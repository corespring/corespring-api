package org.corespring.elasticsearch

import org.corespring.models.json.JsonUtil
import play.api.libs.json.{JsString, Json, JsObject, Writes}

abstract class ElasticSearchWrites[A] extends Writes[A] with JsonUtil {

  protected def terms[A](field: String, values: Seq[A], execution: Option[String] = None)(implicit writes: Writes[A]) = filter("terms", field, values, execution): Option[JsObject]

  protected def term[A](field: String, values: Option[A])(implicit writes: Writes[A], execution: Option[String] = None): Option[JsObject] =
    filter("term", field, values, execution)

  protected def filter[A](named: String, field: String, values: Seq[A], execution: Option[String])(implicit writes: Writes[A]): Option[JsObject] =
    values.nonEmpty match {
      case true => Some(Json.obj(named -> partialObj(
        field -> Some(Json.toJson(values)), "execution" -> execution.map(JsString))))
      case _ => None
    }

  protected def filter[A](named: String, field: String, value: Option[A], execution: Option[String])(implicit writes: Writes[A]): Option[JsObject] =
    value.map(v => partialObj(
      named -> Some(Json.obj(field -> Json.toJson(v))), "execution" -> execution.map(JsString)))

  protected def should(text: Option[String], fields: Seq[String]): Option[JsObject] = text match {
    case Some("") => None
    case Some(text) => Some(Json.obj("should" -> Json.arr(
      Json.obj("multi_match" -> Json.obj(
        "query" -> text,
        "fields" -> fields,
        "type" -> "phrase")),
      Json.obj("ids" -> Json.obj(
        "values" -> Json.arr(text))))))
    case _ => None
  }

}
