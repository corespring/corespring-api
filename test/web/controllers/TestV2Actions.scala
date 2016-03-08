package web.controllers

import org.corespring.models.auth.ApiClient
import org.corespring.v2.actions.{ OrgAndApiClientRequest, OrgRequest, V2Actions }
import org.corespring.v2.auth.models.OrgAndOpts
import play.api.mvc.{ ActionBuilder, Request, SimpleResult }

import scala.concurrent.Future

class TestV2Actions(orgAndOpts: OrgAndOpts, apiClient: ApiClient) {

  lazy val actions = new V2Actions {
    override val Org: ActionBuilder[OrgRequest] = new ActionBuilder[OrgRequest] {
      override protected def invokeBlock[A](request: Request[A], block: (OrgRequest[A]) => Future[SimpleResult]): Future[SimpleResult] = {
        block(OrgRequest(request, orgAndOpts))
      }
    }
    override val OrgAndApiClient: ActionBuilder[OrgAndApiClientRequest] = new ActionBuilder[OrgAndApiClientRequest] {
      override protected def invokeBlock[A](request: Request[A], block: (OrgAndApiClientRequest[A]) => Future[SimpleResult]): Future[SimpleResult] = {
        block(OrgAndApiClientRequest(request, orgAndOpts, apiClient))
      }
    }
    override val RootOrg: ActionBuilder[OrgRequest] = Org
  }
}
