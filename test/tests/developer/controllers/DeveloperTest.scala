package tests.developer.controllers

import com.mongodb.casbah.commons.MongoDBObject
import developer.controllers.Developer
import models.{Organization, User}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.specs2.mutable.After
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.Helpers._
import securesocial.core.SecureSocial
import tests.BaseTest

class DeveloperTest extends BaseTest {

  sequential

  def secureSocialSession(u: Option[User], date: DateTime = DateTime.now()): Array[(String, String)] = u match {
    case Some(user) => Array(
      (SecureSocial.UserKey -> user.userName),
      (SecureSocial.ProviderKey -> user.provider),
      (SecureSocial.LastAccessKey -> date.toString)
    )
    case _ => Array()
  }

  "Developer" should {

    "redirects to organization form when user belongs to demo org" in new MockUser{
      val request = fakeRequest().withSession(secureSocialSession(Some(user)): _*)
      val result = Developer.home(request);
      status(result) must equalTo(SEE_OTHER)
      headers(result).get("Location") must beEqualTo(Some("/developer/org/form"))
    }

    "return developer/home when user has registered org" in new MockUser{
      val orgName = """{"name":"%s"}""".format(testOrgName)
      val json = Json.parse(orgName)
      val createRequest = fakeRequest(AnyContentAsJson(json)).withSession(secureSocialSession(Some(user)): _*)
      Developer.createOrganization()(createRequest)
      val request = fakeRequest().withSession(secureSocialSession(Some(user)): _*)
      val result = Developer.home(request);
      status(result) must equalTo(OK)
    }

    "create only one org" in new MockUser {
      val orgName = """{"name":"%s"}""".format(testOrgName)
      val json = Json.parse(orgName)
      val request = fakeRequest(AnyContentAsJson(json)).withSession(secureSocialSession(Some(user)): _*)
      status(Developer.createOrganization()(request)) === OK
      println(contentAsString(Developer.createOrganization()(request)))
      status(Developer.createOrganization()(request)) === BAD_REQUEST
    }

    "not logged in with expired session" in new MockUser{
      val request = fakeRequest().withSession(secureSocialSession(Some(user), DateTime.now().minusDays(200)): _*)
      val result = Developer.isLoggedIn(request)
      (Json.parse(contentAsString(result)) \ "isLoggedIn") must equalTo(false)
    }
  }
}

class MockUser extends After {

  lazy val oid = ObjectId.get()
  lazy val user = createUser
  lazy val testOrgName = "DeveloperTest-OrgName"

  def createUser = {
    val u = User(userName = "google_user_name", fullName = "some user", email = "some.user@google.com", provider = "google", id = oid)
    User.insert(u)
    u
  }

  def after {
    User.remove(MongoDBObject("userName" -> "google_user_name"))
    Organization.remove(MongoDBObject("name" -> testOrgName))
  }
}
