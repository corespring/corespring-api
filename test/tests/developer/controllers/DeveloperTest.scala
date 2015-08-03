package tests.developer.controllers

import com.mongodb.casbah.commons.MongoDBObject
import developer.controllers.Developer
import java.util.regex.Pattern
import org.bson.types.ObjectId
import org.corespring.common.log.PackageLogging
import org.corespring.platform.core.controllers.auth.AuthController
import org.corespring.models.{ ContentCollection, User, Organization }
import org.corespring.test.{ SecureSocialHelpers, TestModelHelpers, BaseTest }
import org.specs2.mutable.{ Before, After }
import play.api.libs.json.{ JsArray, Json }
import play.api.mvc.{ AnyContentAsFormUrlEncoded, AnyContentAsJson }
import play.api.test.FakeRequest
import play.api.test.Helpers._

class DeveloperTest extends BaseTest
  with SecureSocialHelpers
  with PackageLogging {

  sequential

  "Developer" should {

    "redirects to organization form when user belongs to demo org" in new MockUser {
      val request = fakeRequest().withCookies(secureSocialCookie(Some(user)).toList: _*)
      val result = Developer.home(request)
      status(result) must equalTo(SEE_OTHER)
      headers(result).get("Location") must beEqualTo(Some("/developer/org/form"))
    }

    "return developer/home when user has registered org" in new MockUser {
      val orgName = """{"name":"%s"}""".format(testOrgName)
      val json = Json.parse(orgName)
      val createRequest = fakeRequest(AnyContentAsJson(json)).withCookies(secureSocialCookie(Some(user)).toList: _*)
      val createResult = Developer.createOrganization()(createRequest)
      status(createResult) === OK
      val request = fakeRequest().withCookies(secureSocialCookie(Some(user)).toList: _*)
      val result = Developer.home(request)
      status(result) must equalTo(OK)
    }

    "creates a default collection for the organisation" in new MockUser {
      val orgName = """{"name":"%s"}""".format(testOrgName)
      val json = Json.parse(orgName)
      val request = fakeRequest(AnyContentAsJson(json)).withCookies(secureSocialCookie(Some(user)).toList: _*)
      status(Developer.createOrganization()(request)) === OK
      Organization.findOne(MongoDBObject("name" -> testOrgName))
        .map(
          org => {
            val publicCollectionsRefs = ContentCollection.getPublicCollections.map(col => col.id)

            // Check that newly created organization contains reference to a default collection
            // When created any organization is automatically added all public collections so we will filter them out
            org.contentcolls.filterNot(collRef => publicCollectionsRefs.contains(collRef.collectionId)).length must equalTo(1)

            val defaultCollectionsIds = ContentCollection
              .find(MongoDBObject("name" -> ContentCollection.DEFAULT, "ownerOrgId" -> org.id))
              .toSeq
              .map(coll => coll.id)

            org.contentcolls
              .map(collRef => collRef.collectionId)
              .intersect(defaultCollectionsIds).length must equalTo(1)
          })
        .getOrElse(failure(s"Could not fetch organisation: $testOrgName"))
    }

    "create only one org" in new MockUser {
      val orgName = """{"name":"%s"}""".format(testOrgName)
      val json = Json.parse(orgName)
      val request = fakeRequest(AnyContentAsJson(json)).withCookies(secureSocialCookie(Some(user)).toList: _*)
      status(Developer.createOrganization()(request)) === OK
      status(Developer.createOrganization()(request)) === BAD_REQUEST
    }

    "be able to view org after creating it" in new MockUser {
      val orgName = """{"name":"%s"}""".format(testOrgName)
      val json = Json.parse(orgName)
      val createRequest = fakeRequest(AnyContentAsJson(json)).withCookies(secureSocialCookie(Some(user)).toList: _*)
      val createResult = Developer.createOrganization()(createRequest)
      status(createResult) === OK
      val body = contentAsString(createResult).replaceAll("\\s", "")
      val m = Pattern.compile(
        """.*<p><span class="span2">ClientID:</span>(.*)</p>.*<p><span class="span2">Client Secret:</span>(.*)</p>.*""".replaceAll("\\s", "")).matcher(body)
      m.matches() === true
      val clientSecret = m.group(2)
      val clientId = m.group(1)
      val tokenRequest = fakeRequest(AnyContentAsFormUrlEncoded(Map("client_id" -> Seq(clientId), "client_secret" -> Seq(clientSecret))))
      val tokenResult = AuthController.getAccessToken()(tokenRequest)
      status(tokenResult) === OK
      val token = (Json.parse(contentAsString(tokenResult)) \ "access_token").as[String]
      val listCall = org.corespring.api.v1.routes.OrganizationApi.list()
      val request = FakeRequest(listCall.method, tokenize(listCall.url, token))
      val result = route(request).get
      status(result) === OK
      (Json.parse(contentAsString(result)) match {
        case JsArray(jsorgs) => jsorgs.find(jsorg => (jsorg \ "name").as[String] == testOrgName).isDefined
        case _ => false
      }) === true
    }

    "return unauthorized with expired session" in new MockUser {

      val request = fakeRequest()
        .withCookies(expiredSecureSocialCookie(Some(user)).toSeq: _*)
      val result = Developer.isLoggedIn(request)
      status(result) === UNAUTHORIZED
    }
  }
}

class MockUser extends After with Before {

  lazy val oid = ObjectId.get()
  lazy val user = createUser
  lazy val testOrgName = "DeveloperTest-OrgName"

  def createUser = {
    val u = User(userName = "google_user_name", fullName = "some user", email = "some.user@google.com", provider = "google", id = oid)
    User.insert(u)
    u
  }

  override def before {
    User.remove(MongoDBObject("userName" -> "google_user_name"))
    Organization.remove(MongoDBObject("name" -> testOrgName))
    ContentCollection.remove(MongoDBObject("ownerOrgId" -> oid))
  }

  override def after {
    User.remove(MongoDBObject("userName" -> "google_user_name"))
    Organization.remove(MongoDBObject("name" -> testOrgName))
    ContentCollection.remove(MongoDBObject("ownerOrgId" -> oid))
  }
}
