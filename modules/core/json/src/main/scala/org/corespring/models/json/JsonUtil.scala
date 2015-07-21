package org.corespring.models.json

import play.api.libs.json.{ Json, JsObject, JsValue }

import scalaz.{ Validation, Success, Failure }

trait JsonUtil {

  def partialObj(fields: (String, Option[JsValue])*): JsObject =
    JsObject(fields.filter { case (_, v) => v.nonEmpty }.map { case (a, b) => (a, b.get) })

  def safeParse(string: String): Validation[Error, JsValue] = try {
    Success(Json.parse(string))
  } catch {
    case e: Exception => Failure(new Error(s"Invalid JSON: $string"))
  }

}
