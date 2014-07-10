package org.corespring.api.v2

import org.corespring.api.v2.errors.V2ApiError
import play.api.mvc.{ Controller, SimpleResult }

import scala.concurrent.ExecutionContext
import scalaz.Validation

trait V2Api extends Controller {

  implicit def ec: ExecutionContext

  protected def validationToResult[A](fn: A => SimpleResult)(v: Validation[V2ApiError, A]) = {
    v.fold[SimpleResult]((e) => Status(e.code)(e.message), d => fn(d))
  }
}
