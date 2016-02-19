package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.errors.{ GeneralError, PlatformServiceError }
import org.corespring.models.Organization
import org.corespring.models.auth.{ ApiClient, AccessToken }
import org.corespring.services.OrganizationService
import org.corespring.services.auth.{ AccessTokenService, ApiClientService }
import org.corespring.v2.errors.Errors.{ noToken, _ }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest

import scalaz.{ Failure, Success, Validation }

class TokenOrgIdentityTest extends Specification with Mockito {

  val mockOrg = {
    val m = mock[Organization]
    m.id returns ObjectId.get
    m
  }

  def testError = GeneralError("testError", None)

  "apply" should {

    class scope[A](val apiClientId: ObjectId = ObjectId.get,
      val org: Validation[PlatformServiceError, Organization] = Failure(testError))
      extends Scope {

      lazy val accessToken = AccessToken(apiClientId, mockOrg.id, None, "tokenId")

      lazy val tokenService: AccessTokenService = {
        val m = mock[AccessTokenService]
        m.findByTokenId(any[String]) returns Some(accessToken)
        m
      }

      lazy val orgService: OrganizationService = {
        val m = mock[OrganizationService]
        m.findOneById(any[ObjectId]) returns org.toOption
        m
      }

      lazy val apiClient = ApiClient(mockOrg.id, ObjectId.get, "secret")

      lazy val apiClientService = {
        val m = mock[ApiClientService]
        m.getOrCreateForOrg(any[ObjectId]) returns Success(apiClient)
        m
      }

      val identifier = new TokenOrgIdentity(
        tokenService,
        orgService,
        apiClientService)
    }

    "not work" in new scope[AnyContentAsEmpty.type] {
      identifier.apply(FakeRequest()) must_== Failure(noToken(FakeRequest()))
    }

    "not work with token" in new scope[AnyContentAsEmpty.type] {
      val rh = FakeRequest("", "?access_token=blah")
      identifier.apply(rh) must_== Failure(cantFindOrgWithId(mockOrg.id))
    }

    "work with token + defaultCollection" in new scope[AnyContentAsEmpty.type](
      org = Success(mockOrg)) {
      identifier.apply(FakeRequest("", "?access_token=blah")).map(_.org) must_== Success(mockOrg)
    }

  }

}
