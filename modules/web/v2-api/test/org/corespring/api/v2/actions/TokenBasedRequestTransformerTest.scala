package org.corespring.api.v2.actions

import org.bson.types.ObjectId
import org.corespring.api.v2.services.OrgService
import org.corespring.platform.core.models.Organization
import org.corespring.test.PlaySingleton
import org.corespring.v2.auth.TokenBasedRequestTransformer
import org.corespring.v2.auth.services.TokenService
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest

class TokenBasedRequestTransformerTest extends Specification with Mockito {

  //TODO: This shouldn't be necessary
  PlaySingleton.start()

  "TokenBasedRequestTransformer" should {

    class scope[A](val org: Option[Organization] = None,
      val defaultCollection: Option[ObjectId] = None) extends Scope {

      val transformer = new TokenBasedRequestTransformer[A] {
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
      transformer.apply(FakeRequest("", "?access_token=blah")).map { or =>
        or.defaultCollection === defaultCollection.get
        or.orgId === org.get.id
      }.getOrElse(failure("no org request"))
    }
  }
}
