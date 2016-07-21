package org.corespring.v2.actions

import org.bson.types.ObjectId
import org.corespring.v2.auth.models.{OrgAndOpts, MockFactory}
import org.specs2.mock.Mockito
import play.api.mvc._

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.Success

object V2ActionsFactory extends Mockito with MockFactory {

  val orgCollections = Seq(ObjectId.get, ObjectId.get)
  val orgAndOpts = mockOrgAndOpts(collections = orgCollections)
  val apiClient = mockApiClient(orgAndOpts)

  def invokeOrgActionBuilderMock(fn: Any): Action[AnyContent] = {
    val block = fn.asInstanceOf[Request[_] => Future[SimpleResult]]
    Action.async { req =>
      block(OrgRequest(req, orgAndOpts))
    }
  }

  def apply(orgAndOpts: OrgAndOpts = mockOrgAndOpts(collections = orgCollections)): V2Actions = {

    val m = mock[V2Actions]

    val builder = new OrgActionBuilder(
      v2ActionContext = V2ActionExecutionContext(ExecutionContext.global),
      (rh) => Future.successful(Success(orgAndOpts)),
      None)

    val orgAndClientBuilder = new ActionBuilder[OrgAndApiClientRequest] {
      override protected def invokeBlock[A](request: Request[A], block: (OrgAndApiClientRequest[A]) => Future[SimpleResult]): Future[SimpleResult] = {
        block(OrgAndApiClientRequest(request, orgAndOpts, apiClient))
      }
    }

    m.OrgWithStatusCode(any[Int]) answers {
      code =>
        new OrgActionBuilder(
          v2ActionContext = V2ActionExecutionContext(ExecutionContext.global),
          (rh) => Future.successful(Success(orgAndOpts)),
          Some(code.asInstanceOf[Int]))
    }
    m.Org returns builder
    m.RootOrg returns builder
    m.OrgAndApiClient returns orgAndClientBuilder
  }
}
