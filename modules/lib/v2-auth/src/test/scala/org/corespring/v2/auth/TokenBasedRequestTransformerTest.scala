package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.services.{ OrgService, TokenService }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.{ AnyContentAsEmpty, RequestHeader }
import play.api.test.FakeRequest

class TokenBasedRequestTransformerTest extends Specification with Mockito {

  "TokenBasedRequestTransformer" should {

    class scope[A](val org: Option[Organization] = None,
      val defaultCollection: Option[ObjectId] = None) extends Scope {

      val transformer = new TokenBasedRequestTransformer[String] {
        override def tokenService: TokenService = {
          val m = mock[TokenService]
          m.orgForToken(any[String]) returns org
          m
        }

        override def orgService: OrgService = {
          val m = mock[OrgService]
          m.defaultCollection(any[Organization]) returns defaultCollection
          m
        }

        override def data(rh: RequestHeader, org: Organization, defaultCollection: ObjectId): String = "Worked"
      }
    }

    "not work" in new scope[AnyContentAsEmpty.type] {
      transformer.apply(FakeRequest()) === None
    }

    "not work with token" in new scope[AnyContentAsEmpty.type] {
      transformer.apply(FakeRequest("", "?access_token=blah")) === None
    }

    "not work with token" in new scope[AnyContentAsEmpty.type](Some(Organization())) {
      transformer.apply(FakeRequest("", "?access_token=blah")) === None
    }

    "work with token + defaultCollection" in new scope[AnyContentAsEmpty.type](
      Some(Organization()),
      Some(ObjectId.get)) {
      transformer.apply(FakeRequest("", "?access_token=blah")) === Some("Worked")
    }
  }

}
