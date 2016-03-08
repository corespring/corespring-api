package org.corespring.v2.actions

import org.corespring.models.appConfig.DefaultOrgs
import org.corespring.models.auth.ApiClient
import org.corespring.services.auth.ApiClientService
import org.corespring.v2.actions.V2Actions.GetOrgAndOpts
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Failure, Success, Validation }

case class OrgRequest[A](request: Request[A], orgAndOpts: OrgAndOpts) extends WrappedRequest[A](request) {
  def org = orgAndOpts.org
}

case class OrgAndApiClientRequest[A](request: Request[A], orgAndOpts: OrgAndOpts, apiClient: ApiClient) extends WrappedRequest[A](request) {
  def org = orgAndOpts.org
}

case class V2ActionExecutionContext(context: ExecutionContext)

trait V2Actions {
  val Org: ActionBuilder[OrgRequest]
  val RootOrg: ActionBuilder[OrgRequest]
  val OrgAndApiClient: ActionBuilder[OrgAndApiClientRequest]
}

object V2Actions {
  type GetOrgAndOpts = RequestHeader => Future[Validation[V2Error, OrgAndOpts]]
}

import scala.language.higherKinds

private[actions] abstract class BaseOrgActionBuilder[R[_]](
  v2ActionContext: V2ActionExecutionContext,
  getOrgAndOptsFn: RequestHeader => Future[Validation[V2Error, OrgAndOpts]]) extends ActionBuilder[R] {

  def makeWrappedRequest[A](rh: Request[A], id: OrgAndOpts): Option[R[A]]

  implicit val ec = v2ActionContext.context

  override protected def invokeBlock[A](request: Request[A], block: (R[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    getOrgAndOptsFn(request).flatMap { v =>
      v match {
        case Success(identity) => makeWrappedRequest(request, identity).map { r =>
          block(r)
        }.getOrElse(Future.successful(BadRequest(Json.obj("error" -> "Failed to make request"))))
        case Failure(err) => Future.successful(Status(err.statusCode)(err.json))
      }
    }
  }
}

class OrgActionBuilder(
  v2ActionContext: V2ActionExecutionContext,
  getOrgAndOptsFn: GetOrgAndOpts)
  extends BaseOrgActionBuilder[OrgRequest](v2ActionContext, getOrgAndOptsFn) {
  override def makeWrappedRequest[A](r: Request[A], id: OrgAndOpts): Option[OrgRequest[A]] = Some(OrgRequest(r, id))
}

class RootOrgActionBuilder(defaultOrgs: DefaultOrgs, v2ActionContext: V2ActionExecutionContext, getOrgAndOptsFn: GetOrgAndOpts)
  extends BaseOrgActionBuilder[OrgRequest](v2ActionContext, getOrgAndOptsFn) {
  override def makeWrappedRequest[A](rh: Request[A], id: OrgAndOpts): Option[OrgRequest[A]] = {
    if (id.org.id == defaultOrgs.root) {
      Some(OrgRequest(rh, id))
    } else {
      None
    }
  }
}

class OrgAndApiClientActionBuilder(apiClientService: ApiClientService,
  v2ActionContext: V2ActionExecutionContext,
  getOrgAndOpts: GetOrgAndOpts)
  extends BaseOrgActionBuilder[OrgAndApiClientRequest](v2ActionContext, getOrgAndOpts) {

  override def makeWrappedRequest[A](rh: Request[A], id: OrgAndOpts): Option[OrgAndApiClientRequest[A]] = {
    id.apiClientId match {
      case None => {
        apiClientService.getOrCreateForOrg(id.org.id).map { c =>
          OrgAndApiClientRequest(rh, id, c)
        }.toOption
      }
      case Some(clientId) => {
        apiClientService.findByClientId(clientId).map { c =>
          OrgAndApiClientRequest(rh, id, c)
        }
      }
    }
  }
}

class DefaultV2Actions(
  defaultOrgs: DefaultOrgs,
  getOrgAndOptsFn: RequestHeader => Future[Validation[V2Error, OrgAndOpts]],
  apiClientService: ApiClientService,
  v2ActionContext: V2ActionExecutionContext) extends V2Actions {

  implicit val ec = v2ActionContext.context

  import play.api.mvc.Results.Status

  protected implicit class V2ErrorWithSimpleResult(error: V2Error) {
    def toResult: SimpleResult = Status(error.statusCode)(error.json)

    def toResult(statusCode: Int): SimpleResult = Status(statusCode)(error.json)
  }

  override lazy val Org = new OrgActionBuilder(v2ActionContext, getOrgAndOptsFn)

  override lazy val OrgAndApiClient: ActionBuilder[OrgAndApiClientRequest] = {
    new OrgAndApiClientActionBuilder(apiClientService, v2ActionContext, getOrgAndOptsFn)
  }

  override lazy val RootOrg = new RootOrgActionBuilder(defaultOrgs, v2ActionContext, getOrgAndOptsFn)

}
