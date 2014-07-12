package org.corespring.v2.api

import org.corespring.v2.errors.V2Error
import play.api.mvc.{ Controller, SimpleResult }

import scala.concurrent.ExecutionContext
import scalaz.Validation

trait V2Api extends Controller {

  implicit def ec: ExecutionContext

  protected def validationToResult[A](fn: A => SimpleResult)(v: Validation[V2Error, A]) = {
    def errResult(e: V2Error): SimpleResult = Status(e.statusCode)(e.json)
    v.fold[SimpleResult](errResult, fn)
  }
}
