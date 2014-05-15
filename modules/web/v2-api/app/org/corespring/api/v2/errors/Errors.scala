package org.corespring.api.v2.errors

import play.api.libs.json.{Json, JsValue}

sealed abstract class V2ApiError(val code: Int, val message: String)

object Errors {

  import play.api.http.Status._

  case class generalError(c: Int, msg: String) extends V2ApiError(c, msg)

  object noJson extends V2ApiError(BAD_REQUEST, "No json in request body")
  case class invalidJson(str:String) extends V2ApiError(BAD_REQUEST, s"Invalid json $str")

  case class unAuthorized(errors:String*) extends V2ApiError(UNAUTHORIZED, errors.mkString(", "))

  object errorSaving extends V2ApiError(BAD_REQUEST, "Error saving")

  case class incorrectJsonFormat(json:JsValue) extends V2ApiError(BAD_REQUEST, s"Bad json format ${Json.stringify(json)}")
}
