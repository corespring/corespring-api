package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.services.{ OrgService, TokenService }
import org.corespring.v2.errors.Errors.{ generalError, noToken, noDefaultCollection }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.{ AnyContentAsEmpty, RequestHeader }
import play.api.test.FakeRequest

import scalaz.{ Failure, Success }

class TokenOrgIdentityTest extends Specification with Mockito {

  def mockOrg = {
    val m = mock[Organization]
    m.id returns ObjectId.get
    m
  }

  "TokenBasedRequestTransformer" should {

    class scope[A](val org: Option[Organization] = None,
      val defaultCollection: Option[ObjectId] = None) extends Scope {

      val transformer = new TokenOrgIdentity[String] {
        override def tokenService: TokenService = {
          val m = mock[TokenService]
          m.orgForToken(any[String]) returns org
          m
        }

        override def orgService: OrgService = {
          val m = mock[OrgService]
          m.defaultCollection(any[Organization]) returns defaultCollection
          m.org(any[ObjectId]) returns org
          m
        }

        override def data(rh: RequestHeader, org: Organization, defaultCollection: ObjectId): String = "Worked"
      }
    }

    "not work" in new scope[AnyContentAsEmpty.type] {
      transformer.apply(FakeRequest()) must_== Failure(noToken(FakeRequest()))
    }

    "not work with token" in new scope[AnyContentAsEmpty.type] {
      transformer.apply(FakeRequest("", "?access_token=blah")) must_== Failure(generalError("Can't find org for token blah"))
    }

    "not work with token" in new scope[AnyContentAsEmpty.type](Some(mockOrg)) {
      transformer.apply(FakeRequest("", "?access_token=blah")) must_== Failure(noDefaultCollection(org.get.id))
    }

    "work with token + defaultCollection" in new scope[AnyContentAsEmpty.type](
      Some(mockOrg),
      Some(ObjectId.get)) {
      transformer.apply(FakeRequest("", "?access_token=blah")) must_== Success("Worked")
    }
  }

}
