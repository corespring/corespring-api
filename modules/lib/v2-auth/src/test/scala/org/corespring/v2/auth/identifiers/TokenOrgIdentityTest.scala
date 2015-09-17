package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.models.{ User, Organization }
import org.corespring.services.OrganizationService
import org.corespring.services.auth.AccessTokenService
import org.corespring.services.errors.{ GeneralError, PlatformServiceError }
import org.corespring.v2.errors.Errors.{ noToken }
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.{ AnyContentAsEmpty, RequestHeader }
import play.api.test.FakeRequest
import org.corespring.v2.errors.Errors._

import scalaz.{ Validation, Failure, Success }

class TokenOrgIdentityTest extends Specification with Mockito {

  import org.mockito.Matchers._

  def mockOrg = {
    val m = mock[Organization]
    m.id returns ObjectId.get
    m
  }

  def testError = GeneralError("testError", None)

  "TokenBasedRequestTransformer" should {

    class scope[A](val org: Validation[PlatformServiceError, Organization] = Failure(testError),
      val defaultCollection: Option[ObjectId] = None) extends Scope {

      lazy val tokenService: AccessTokenService = {
        val m = mock[AccessTokenService]
        m.orgForToken(any[String]) returns org
        m
      }

      lazy val orgService: OrganizationService = {
        val m = mock[OrganizationService]
        m.defaultCollection(any[Organization]) returns defaultCollection
        m.findOneById(any[ObjectId]) returns org.toOption
        m
      }

      class MockIdentity extends TokenOrgIdentity[String](tokenService, orgService) {
        /** convert the header, org and defaultCollection into the expected output type B */
        override def data(rh: RequestHeader, org: Organization, apiClientId: Option[String], user: Option[User]): Validation[V2Error, String] = Success("Worked")
      }
      val transformer = new MockIdentity()
    }

    "not work" in new scope[AnyContentAsEmpty.type] {
      transformer.apply(FakeRequest()) must_== Failure(noToken(FakeRequest()))
    }

    "not work with token" in new scope[AnyContentAsEmpty.type] {
      val rh = FakeRequest("", "?access_token=blah")
      transformer.apply(rh) must_== Failure(noOrgForToken(rh))
    }

    "work with token + defaultCollection" in new scope[AnyContentAsEmpty.type](
      Success(mockOrg),
      Some(ObjectId.get)) {
      transformer.apply(FakeRequest("", "?access_token=blah")) must_== Success("Worked")
    }
  }

}
