package org.corespring.v2.actions

import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import play.api.mvc._

import scala.concurrent.Future
import scalaz.{ Failure, Success }

trait V2Actions extends LoadOrgAndOptions {

  import play.api.mvc.Results.Status

  protected implicit class V2ErrorWithSimpleResult(error: V2Error) {
    def toResult: SimpleResult = Status(error.statusCode)(error.json)
    def toResult(statusCode: Int): SimpleResult = Status(statusCode)(error.json)
  }

  def OrgAction(block: (OrgAndOpts, Request[AnyContent]) => Future[SimpleResult]): Action[AnyContent] = {
    Action.async { implicit request =>
      getOrgAndOptions(request) match {
        case Success(identity) => block(identity, request)
        case Failure(e) => Future { e.toResult }
      }
    }
  }
}
