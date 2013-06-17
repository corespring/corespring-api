package tests.developer.controllers

import developer.controllers.Developer
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.{FakeHeaders, FakeRequest}
import tests.BaseTest
import models.User
import org.specs2.specification.After
import org.bson.types.ObjectId

class DeveloperTest extends BaseTest{

  sequential

  "Developer" should{

    "create only one org" in new MockUser {

      val orgName = """{"name":"hello-there"}"""
      val json = Json.parse(orgName)

        /** PLAY_SESSION=b94eb099c66b16190b716dcfc82b7910e67a1a47-
          *
      securesocial.user%3A109165101605322411454%00
        player.active.mode%3Apreview%00
        securesocial.id%3A174c606b-69dd-4258-851e-a96e3ffca674%00
        securesocial.provider%3Agoogle%00
        securesocial.lastAccess%3A2013-06-17T13%3A05%3A06.017%2B02%3A00
          */
      //securesocial.id%3A174c606b-69dd-4258-851e-a96e3ffca674%00securesocial.provider%3Agoogle%00securesocial.lastAccess%3A2013-06-17T13%3A05%3A06.017%2B02%3A00


      val request = FakeRequest("", tokenize("blah"), FakeHeaders(), AnyContentAsJson(json))
        Developer.createOrganization()(request)
    }
  }

}

class MockUser extends After {

  lazy val oid = ObjectId.get()
  lazy val user = createUser

  def createUser = {
    val u = User("google_user_name", "some user", "some.user@google.com", Seq(), "", provider = "google", hasRegisteredOrg = false, id = oid)
    User.insert(u)
    u
  }

  def after{
   User.remove(user)
  }
}
