package org.corespring.v2.api

import org.corespring.errors.PlatformServiceError
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Failure, Success, Validation }

trait ValidationToResultLike {

  import play.api.http.Status.OK
  import play.api.mvc.Results.Status

  protected def validationToFutureResult[A](fn: Future[A] => Future[SimpleResult])(v: Validation[V2Error, Future[A]]) = {
    def errResult(e: V2Error) = Future.successful(jsonResult(e.statusCode, e.json))
    v.fold[Future[SimpleResult]](errResult, fn)
  }

  protected implicit class V2ErrorWithSimpleResult(error: V2Error) {
    def toResult: SimpleResult = Status(error.statusCode)(error.json)
    def toResult(statusCode: Int): SimpleResult = Status(statusCode)(error.json)
  }

  type FVE = Future[Validation[V2Error, JsValue]]

  protected implicit class FutureValidationToSimpleResult(f: FVE) {
    def toSimpleResult(statusCode: Int = OK)(implicit ec: ExecutionContext): Future[SimpleResult] = {
      f.map { v =>
        v match {
          case Success(json) => Status(statusCode)(json)
          case Failure(e) => Status(e.statusCode)(e.json)
        }
      }
    }
  }

  protected implicit class ValidationToSimpleResult(v: Validation[V2Error, JsValue]) {
    def toSimpleResult(statusCode: Int = OK): SimpleResult = {
      v match {
        case Success(json) => Status(statusCode)(json)
        case Failure(e) => Status(e.statusCode)(e.json)
      }
    }
  }

  private def jsonResult(statusCode: Int, json: JsValue) = Status(statusCode)(json)
}

trait V2Api extends Controller with ValidationToResultLike {

  implicit def ec: ExecutionContext

  protected implicit class MkV2Error[A](v: Validation[PlatformServiceError, A]) {

    require(v != null)

    def v2Error: Validation[V2Error, A] = {
      v.leftMap { e => generalError(e.message) }
    }
  }

}
