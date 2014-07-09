package org.corespring.api.v2

import org.corespring.api.v2.errors.Errors.unAuthorized
import org.corespring.api.v2.errors.V2ApiError
import org.corespring.api.v2.services.{ Denied, Granted, PermissionResult }
import play.api.mvc.{ Controller, SimpleResult }

import scala.concurrent.ExecutionContext
import scalaz.{ Failure, Success, Validation }

trait V2Api extends Controller {

  implicit def ec: ExecutionContext

  protected def toValidation(r: PermissionResult): Validation[V2ApiError, Boolean] = {
    r match {
      case Granted => Success(true)
      case Denied(reasons) => Failure(unAuthorized(reasons))
    }
  }

  protected def validationToResult[A](fn: A => SimpleResult)(v: Validation[V2ApiError, A]) = {
    v.fold[SimpleResult]((e) => Status(e.code)(e.message), d => fn(d))
  }
}
