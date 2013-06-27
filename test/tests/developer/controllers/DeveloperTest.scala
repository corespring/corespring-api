package tests.developer.controllers

import developer.controllers.Developer
import models.User
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.specs2.specification.After
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import securesocial.core.SecureSocial
import tests.BaseTest

class DeveloperTest extends BaseTest {

  sequential


  def secureSocialSession(u: Option[User]): Array[(String, String)] = u match {
    case Some(user) => Array(
      (SecureSocial.UserKey -> user.userName),
      (SecureSocial.ProviderKey -> user.provider),
      (SecureSocial.LastAccessKey -> DateTime.now().toString)
    )
    case _ => Array()
  }

  "Developer" should {

    "create only one org" in new MockUser {
      val orgName = """{"name":"hello-there"}"""
      val json = Json.parse(orgName)
      val request = FakeRequest("", "", FakeHeaders(), AnyContentAsJson(json)).withSession(secureSocialSession(Some(user)): _*)
      status(Developer.createOrganization()(request)) === OK
      status(Developer.createOrganization()(request)) === BAD_REQUEST
    }.pendingUntilFixed("coming soon.")

    "be able to login" in {

      pending
    }
  }

}

class MockUser extends After {

  lazy val oid = ObjectId.get()
  lazy val user = createUser

  def createUser = {
    val u = User(userName = "google_user_name", fullName = "some user", email = "some.user@google.com", provider = "google", id = oid)
    User.insert(u)
    u
  }

  def after {
    User.remove(user)
  }
}
