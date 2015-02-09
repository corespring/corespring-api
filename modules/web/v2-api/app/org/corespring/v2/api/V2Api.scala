package org.corespring.v2.api

import org.corespring.v2.auth.{LoadApiClient, LoadOrgAndOptions}
import org.corespring.v2.errors.V2Error
import play.api.mvc.{ Controller, SimpleResult }

import scala.concurrent.ExecutionContext
import scalaz.Validation

trait V2Api extends Controller with LoadOrgAndOptions with LoadApiClient {

  implicit def ec: ExecutionContext

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
}
