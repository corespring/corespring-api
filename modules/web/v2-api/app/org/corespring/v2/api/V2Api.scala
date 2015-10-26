package org.corespring.v2.api

import org.corespring.errors.PlatformServiceError
import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.{ generalError, noToken }
import org.corespring.v2.errors.V2Error
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc._

import scala.concurrent.{ Future, ExecutionContext }
import scalaz.{ Failure, Success, Validation }

trait V2Api extends Controller with LoadOrgAndOptions {

  implicit def ec: ExecutionContext

  def getOrgAndOptionsFn: RequestHeader => Validation[V2Error, OrgAndOpts]

  final override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = getOrgAndOptionsFn(request)

  protected implicit class MkV2Error[A](v: Validation[PlatformServiceError, A]) {

    require(v != null)

    def v2Error: Validation[V2Error, A] = {
      v.leftMap { e => generalError(e.message) }
    }
  }

  /**
   * Convert a Validation to a SimpleResult.
   * @param fn the function to convert A to a SimpleResult
   * @tparam A
   * @return
   */
  protected def validationToResult[A](fn: A => SimpleResult)(v: Validation[V2Error, A]) = {
    def errResult(e: V2Error): SimpleResult = Status(e.statusCode)(e.json)
    v.fold[SimpleResult](errResult, fn)
  }

  protected implicit class V2ErrorWithSimpleResult(error: V2Error) {
    def toResult: SimpleResult = Status(error.statusCode)(Json.prettyPrint(error.json))
  }

  protected implicit class ValidationToSimpleResult(v: Validation[V2Error, JsValue]) {
    def toSimpleResult(statusCode: Int = OK): SimpleResult = {
      v match {
        case Success(json) => Status(statusCode)(json)
        case Failure(e) => Status(e.statusCode)(e.json)
      }
    }
  }

  protected def withIdentity(block: (OrgAndOpts, Request[AnyContent]) => SimpleResult) =
    Action.async { implicit request =>
      Future {
        getOrgAndOptions(request) match {
          case Success(identity) => block(identity, request)
          case Failure(e) => e.toResult
        }
      }
    }

  protected def futureWithIdentity(block: (OrgAndOpts, Request[AnyContent]) => Future[SimpleResult]) =
    Action.async { implicit request =>
      getOrgAndOptions(request) match {
        case Success(identity) => block(identity, request)
        case Failure(e) => Future { e.toResult }
      }
    }

}
