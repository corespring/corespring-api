package tests.developer.controllers

import com.mongodb.casbah.commons.MongoDBObject
import common.log.PackageLogging
import developer.controllers.Developer
import models.{Organization, User}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.specs2.mutable.After
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.Helpers._
import tests.BaseTest
import tests.helpers.TestModelHelpers

class DeveloperTest extends BaseTest with TestModelHelpers with PackageLogging{

  sequential

  "Developer" should {

    "redirects to organization form when user belongs to demo org" in new MockUser{
      val request = fakeRequest().withCookies(secureSocialCookie(Some(user)).toList : _*)
      val result = Developer.home(request)
      status(result) must equalTo(SEE_OTHER)
      headers(result).get("Location") must beEqualTo(Some("/developer/org/form"))
    }

    "return developer/home when user has registered org" in new MockUser{
      val orgName = """{"name":"%s"}""".format(testOrgName)
      val json = Json.parse(orgName)
      val createRequest = fakeRequest(AnyContentAsJson(json)).withCookies(secureSocialCookie(Some(user)).toList : _*)
      val createResult = Developer.createOrganization()(createRequest)
      status(createResult) === OK
      val request = fakeRequest().withCookies(secureSocialCookie(Some(user)).toList : _*)
      val result = Developer.home(request)
      status(result) must equalTo(OK)
    }

    "create only one org" in new MockUser {
      val orgName = """{"name":"%s"}""".format(testOrgName)
      val json = Json.parse(orgName)
      val request = fakeRequest(AnyContentAsJson(json)).withCookies(secureSocialCookie(Some(user)).toList : _*)
      status(Developer.createOrganization()(request)) === OK
      status(Developer.createOrganization()(request)) === BAD_REQUEST
    }

    "return unauthorized with expired session" in new MockUser{

      import DateTime.now

      val request = fakeRequest()
        .withCookies(expiredSecureSocialCookie(Some(user)).toSeq : _*)
      val result = Developer.isLoggedIn(request)
      status(result) === UNAUTHORIZED
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
