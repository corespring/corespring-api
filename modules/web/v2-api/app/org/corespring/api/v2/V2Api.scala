package org.corespring.api.v2

import org.corespring.api.v2.errors.V2ApiError
import play.api.libs.json.Json
import play.api.mvc.{ Controller, SimpleResult }

import scala.concurrent.ExecutionContext
import scalaz.Validation

trait V2Api extends Controller {

  implicit def ec: ExecutionContext

  protected def validationToResult[A](fn: A => SimpleResult)(v: Validation[V2ApiError, A]) = {
    def errResult(e: V2ApiError): SimpleResult = Status(e.code)(Json.toJson(e.message))
    v.fold[SimpleResult](errResult, fn)
  }
}
