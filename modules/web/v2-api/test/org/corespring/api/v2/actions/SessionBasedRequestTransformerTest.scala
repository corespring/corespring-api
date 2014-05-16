package org.corespring.api.v2.actions

import org.bson.types.ObjectId
import org.corespring.api.v2.services.OrgService
import org.corespring.platform.core.controllers.auth.SecureSocialService
import org.corespring.platform.core.models.{Organization, User}
import org.corespring.platform.core.services.UserService
import org.corespring.test.PlaySingleton
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.{AnyContentAsEmpty, RequestHeader}
import play.api.test.FakeRequest
import securesocial.core.{Identity, IdentityId}

class SessionBasedRequestTransformerTest extends Specification with Mockito {

  //TODO: This shouldn't be necessary
  PlaySingleton.start()

  class scope[A](
                  val org:Option[Organization] = None,
                  val user:Option[User] = None,
                  val identity: Option[Identity] = None,
                  val defaultCollection : Option[ObjectId] = None) extends Scope{
    val transformer = new SessionBasedRequestTransformer[A]{
      override def orgService: OrgService = {
        val m = mock[OrgService]
        m.org(any[ObjectId]) returns org
        m.defaultCollection(any[Organization]) returns defaultCollection
        m
      }

      override def userService: UserService = {
        val m = mock[UserService]
        m.getUser(any[String], any[String]) returns user
        m
      }

      override def secureSocialService: SecureSocialService = {
        val m = mock[SecureSocialService]
        m.currentUser(any[RequestHeader]) returns identity
        m
      }
    }
  }

  def socialUser : Identity = {
    val m = mock[Identity]
    m.identityId returns(IdentityId("userId", "providerId"))
    m
  }

  "SessionBasedRequestTransformer" should {

    "not return an org request" in new scope[AnyContentAsEmpty.type](){
      transformer.apply(FakeRequest("","")) === None
    }

    "not return an org request org only" in new scope[AnyContentAsEmpty.type](Some(new Organization())){
      transformer.apply(FakeRequest("","")) === None
    }

    "not return an org request, org + user only" in new scope[AnyContentAsEmpty.type](Some(Organization()), Some(User())){
      transformer.apply(FakeRequest("","")) === None
    }

    "not return an org request, org, user and identity only" in new scope[AnyContentAsEmpty.type](
      Some(Organization()),
      Some(User()),
      Some(socialUser)){
      transformer.apply(FakeRequest("","")) === None
    }

    "return an org request, for org, user and identity and default collection" in new scope[AnyContentAsEmpty.type](
      Some(Organization()),
      Some(User()),
      Some(socialUser),
      Some(ObjectId.get)
    ){
      val orgRequest = transformer.apply(FakeRequest("",""))
      orgRequest must beSome[OrgRequest[AnyContentAsEmpty.type]]
      orgRequest.map{ or =>
        or.defaultCollection === defaultCollection.get
        or.orgId === org.get.id
      }.getOrElse(failure("No org request"))
    }
  }

}
