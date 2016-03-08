package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.v2.actions.{ OrgAndApiClientRequest, OrgRequest, V2Actions }
import org.corespring.v2.auth.models.MockFactory
import org.specs2.mock.Mockito
import play.api.mvc.{ ActionBuilder, Request, SimpleResult }

import scala.concurrent.Future

object TestV2Actions extends Mockito with MockFactory {

  val orgCollections = Seq(ObjectId.get, ObjectId.get)
  val orgAndOpts = mockOrgAndOpts(collections = orgCollections)
  val apiClient = mockApiClient(orgAndOpts)

  def apply: V2Actions = {

    val m = mock[V2Actions]

    val builder = new ActionBuilder[OrgRequest] {
      override protected def invokeBlock[A](request: Request[A], block: (OrgRequest[A]) => Future[SimpleResult]): Future[SimpleResult] = {
        block(OrgRequest(request, orgAndOpts))
      }
    }

    val orgAndClientBuilder = new ActionBuilder[OrgAndApiClientRequest] {
      override protected def invokeBlock[A](request: Request[A], block: (OrgAndApiClientRequest[A]) => Future[SimpleResult]): Future[SimpleResult] = {
        block(OrgAndApiClientRequest(request, orgAndOpts, apiClient))
      }
    }

    m.Org returns builder
    m.RootOrg returns builder
    m.OrgAndApiClient returns orgAndClientBuilder
  }
}
