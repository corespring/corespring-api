package org.corespring.v2.actions

import org.corespring.models.Organization
import org.corespring.models.appConfig.DefaultOrgs
import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import play.api.mvc._
import play.api.mvc.Results._

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Validation, Failure, Success }

case class OrgRequest[A](request: Request[A], orgAndOpts: OrgAndOpts) extends WrappedRequest[A](request) {
  def org = orgAndOpts.org
}

class V2Actions(
  defaultOrgs: DefaultOrgs,
  getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts],
  implicit val executionContext: ExecutionContext) extends LoadOrgAndOptions {

  import play.api.mvc.Results.Status

  protected implicit class V2ErrorWithSimpleResult(error: V2Error) {
    def toResult: SimpleResult = Status(error.statusCode)(error.json)
    def toResult(statusCode: Int): SimpleResult = Status(statusCode)(error.json)
  }

  object OrgAction {

    def apply(block: OrgRequest[AnyContent] => SimpleResult): Action[AnyContent] = Action { request =>
      getOrgAndOptions(request) match {
        case Success(identity) => block(OrgRequest(request, identity))
        case Failure(e) => e.toResult
      }
    }

    def async(block: OrgRequest[AnyContent] => Future[SimpleResult]) = Action.async { implicit request =>
      getOrgAndOptions(request) match {
        case Success(identity) => block(OrgRequest(request, identity))
        case Failure(e) => Future.successful { e.toResult }
      }
    }
  }

  object RootOrgAction {
    def apply(block: OrgRequest[AnyContent] => SimpleResult) = Action { request =>
      getOrgAndOptions(request) match {
        case Success(orgAndOpts) if (orgAndOpts.org.id == defaultOrgs.root) => block(OrgRequest(request, orgAndOpts))
        case Success(orgAndOpts) => Unauthorized(s"Org: ${orgAndOpts.org.name} is not the RootOrg")
        case Failure(e) => Unauthorized(e.message)
      }
    }

    def async(block: OrgRequest[AnyContent] => Future[SimpleResult]) = Action.async { request =>
      getOrgAndOptions(request) match {
        case Success(orgAndOpts) if (orgAndOpts.org.id == defaultOrgs.root) => block(OrgRequest(request, orgAndOpts))
        case Success(orgAndOpts) => Future.successful(Unauthorized(s"Org: ${orgAndOpts.org.name} is not the RootOrg"))
        case Failure(e) => Future.successful(Unauthorized(e.message))
      }
    }
  }

  override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = getOrgAndOptsFn(request)
}
