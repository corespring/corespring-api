package org.corespring.v2.actions

import org.bson.types.ObjectId
import org.corespring.models.appConfig.DefaultOrgs
import org.corespring.models.auth.ApiClient
import org.corespring.services.auth.ApiClientService
import org.corespring.v2.auth.models.{ MockFactory, OrgAndOpts }
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.RequestHeader
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Failure, Success, Validation }

class OrgAndApiClientBuilderTest extends Specification with MockFactory with Mockito {

  trait scope extends Scope {
    lazy val req = FakeRequest()
    lazy val orgAndOpts = mockOrgAndOpts()
    lazy val result: Validation[V2Error, OrgAndOpts] = Success(orgAndOpts)
    def getOrgAndOpts(rh: RequestHeader) = Future.successful(result)
    lazy val defaultOrgs = DefaultOrgs(Nil, orgAndOpts.org.id)
    val v2ActionExecutionContext = V2ActionExecutionContext(ExecutionContext.global)
    lazy val apiClient = ApiClient(orgAndOpts.org.id, ObjectId.get, "clientSecret")
    lazy val service = {
      val m = mock[ApiClientService]
      m.getOrCreateForOrg(any[ObjectId]) returns Success(apiClient)
      m.findByClientId(any[String]) returns Some(apiClient)
      m
    }

    val builder = new OrgAndApiClientActionBuilder(service, v2ActionExecutionContext, getOrgAndOpts(_))
  }

  "return action result for root org" in new scope {
    val r = builder.apply(r => Ok("success!"))(req)
    status(r) must_== OK
    contentAsString(r) must_== "success!"
  }

  "return an error if apiClient can't be found" in new scope {
    override lazy val orgAndOpts = mockOrgAndOpts().copy(apiClientId = Some("clientId"))
    service.findByClientId(any[String]) returns None
    val r = builder.apply(r => Ok("success!"))(req)
    status(r) must_== BAD_REQUEST
    contentAsJson(r) must_== generalError("Can't find api client with id: clientId").json
  }

  "return an error if apiClient can't be created" in new scope {
    val err = "test error"
    service.getOrCreateForOrg(any[ObjectId]) returns Failure(err)
    val r = builder.apply(r => Ok("success!"))(req)
    status(r) must_== generalError(err).statusCode
    contentAsJson(r) must_== generalError(err).json
  }
}

