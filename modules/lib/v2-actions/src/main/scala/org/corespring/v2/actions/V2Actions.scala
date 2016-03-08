package org.corespring.v2.actions

import org.corespring.models.appConfig.DefaultOrgs
import org.corespring.models.auth.ApiClient
import org.corespring.services.auth.ApiClientService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import play.api.mvc._
import play.api.mvc.Results._

import scala.concurrent.{ExecutionContext, Future}
import scalaz.{Failure, Success, Validation}

case class OrgRequest[A](request: Request[A], orgAndOpts: OrgAndOpts) extends WrappedRequest[A](request) {
  def org = orgAndOpts.org
}

case class OrgAndApiClientRequest[A](request: Request[A], orgAndOpts: OrgAndOpts, apiClient : ApiClient) extends WrappedRequest[A](request) {
  def org = orgAndOpts.org
}

trait CorespringAction[R <: Request[AnyContent]] {
  def apply(block: R => SimpleResult): Action[AnyContent]
  def async(block: R => Future[SimpleResult]): Action[AnyContent]
}

trait OrgAction extends CorespringAction[OrgRequest[AnyContent]]
trait RootOrgAction extends CorespringAction[OrgRequest[AnyContent]]

case class V2ActionExecutionContext(context: ExecutionContext)

trait V2Actions {
  val Org: ActionBuilder[OrgRequest]
  val RootOrg: RootOrgAction
  val OrgAndApiClient : ActionBuilder[OrgAndApiClientRequest]
}

class OrgActionBuilder(
  v2ActionContext: V2ActionExecutionContext,
  getOrgAndOptsFn: RequestHeader => Future[Validation[V2Error, OrgAndOpts]]) extends ActionBuilder[OrgRequest] {

  implicit val ec = v2ActionContext.context

  override protected def invokeBlock[A](request: Request[A], block: (OrgRequest[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    getOrgAndOptsFn(request).flatMap { v =>
      v match {
        case Success(identity) => block(OrgRequest(request, identity))
        case Failure(err) => Future.successful(Status(err.statusCode)(err.message))
      }
    }
  }
}

class OrgAndApiClientActionBuilder(apiClientService:ApiClientService,
                                   v2ActionContext : V2ActionExecutionContext,
                                   orgActionBuilder : OrgActionBuilder)
extends ActionBuilder[OrgAndApiClientRequest] {
  override protected def invokeBlock[A](request: Request[A], block: (OrgAndApiClientRequest[A]) => Future[SimpleResult]): Future[SimpleResult] = {

    orgActionBuilder.async{ request =>

      apiClientService.findByClientId(request.orgAndOpts.apiClientId) match {
        case None => Future.successful(BadRequest())
        case Some(?)
      }
    }
  }
}

class DefaultV2Actions(
  defaultOrgs: DefaultOrgs,
  getOrgAndOptsFn: RequestHeader => Future[Validation[V2Error, OrgAndOpts]],
  apiClientService : ApiClientService,
  v2ActionContext: V2ActionExecutionContext) extends V2Actions {

  implicit val ec = v2ActionContext.context

  import play.api.mvc.Results.Status

  protected implicit class V2ErrorWithSimpleResult(error: V2Error) {
    def toResult: SimpleResult = Status(error.statusCode)(error.json)
    def toResult(statusCode: Int): SimpleResult = Status(statusCode)(error.json)
  }

  override val OrgAndApiClient: ActionBuilder[OrgAndApiClientRequest] = ???
  lazy val Org = new OrgActionBuilder(v2ActionContext, getOrgAndOptsFn)
  //  {
  //
  //    def apply(block: OrgRequest[AnyContent] => SimpleResult): Action[AnyContent] = async(
  //      r => Future(block(r))
  //    )
  //
  //
  //    override def async(block: (OrgRequest[AnyContent]) => Future[SimpleResult]): Action[AnyContent] = {
  //      Action.async { implicit request : Request[AnyContent] =>
  //        getOrgAndOptsFn(request).flatMap { v => v match {
  //          case Success(identity) => block(OrgRequest(request, identity))
  //          case Failure(e) => Future.successful {
  //            e.toResult
  //          }
  //        }
  //        }
  //      }
  //    }
  //
  //  }

  lazy val RootOrg = new RootOrgAction {

    def apply(block: OrgRequest[AnyContent] => SimpleResult) = async {
      r => Future(block(r))
    }

    def async(block: OrgRequest[AnyContent] => Future[SimpleResult]) = Action.async { request =>
      getOrgAndOptsFn(request).flatMap { v =>
        v match {
          case Success(orgAndOpts) if (orgAndOpts.org.id == defaultOrgs.root) => block(OrgRequest(request, orgAndOpts))
          case Success(orgAndOpts) => Future.successful(Unauthorized(s"Org: ${orgAndOpts.org.name} is not the RootOrg"))
          case Failure(e) => Future.successful(Unauthorized(e.message))
        }
      }
    }
  }

}
