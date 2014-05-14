package org.corespring.api.v2.errors

sealed abstract class V2ApiError(val code: Int, val message: String)

object Errors {

  import play.api.http.Status._

  case class generalError(c: Int, msg: String) extends V2ApiError(c, msg)

  object noJson extends V2ApiError(BAD_REQUEST, "No json in request body")

  object errorSaving extends V2ApiError(BAD_REQUEST, "Error saving")

}
