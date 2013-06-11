package tests.models

import controllers.auth.Permission
import models._
import models.item.Item.Keys
import org.bson.types.ObjectId
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, AnyContentAsEmpty, AnyContentAsJson}
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tests.BaseTest
import tests.helpers.TestModelHelpers

class PermissionsTest extends BaseTest with TestModelHelpers {

  private val log: Logger = Logger(this.getClass.getSimpleName)

  sequential

  def request(token: String, content: AnyContent = AnyContentAsEmpty): FakeRequest[AnyContent] = FakeRequest("", "url?access_token=" + token, FakeHeaders(), content)

  "read" should {

    "not read an organization with no permission" in new TestOPlenty(Permission.None) {
      val result = api.v1.OrganizationApi.listWithOrg(org.id, None, None, "false", 0, 50, None)(request(tokenId))
      log.debug("result: " + status(result).toString)
      status(result) === UNAUTHORIZED
    }

    "read an organization with read permission" in new TestOPlenty(Permission.Read) {
      val result = api.v1.OrganizationApi.getOrganization(org.id)(request(tokenId))
      status(result) === OK
      val jsonString = contentAsString(result)
      Logger.debug(jsonString)
      val name = (Json.parse(jsonString) \ Organization.name).as[String]
      name === org.name
    }

    "not read a collection of items with no permission" in new TestOPlenty(Permission.None) {
      val fakeRequest = FakeRequest(GET, "/api/v1/collections/" + collection.id.toString + "/items?access_token=" + tokenId)
      val result = routeAndCall(fakeRequest).get
      status(result) === UNAUTHORIZED
    }

    "can read a collection of items with read permission" in new TestOPlenty(Permission.Read, Permission.Read) {
      val fakeRequest = FakeRequest(GET, "/api/v1/collections/" + collection.id.toString + "/items?access_token=" + tokenId)
      val result = routeAndCall(fakeRequest).get
      status(result) === OK
    }
  }

  "create" should {
    "not create a collection in an organization with read permission" in new TestOPlenty(Permission.Read) {
      val json = AnyContentAsJson(Json.toJson(new ContentCollection("test")))
      val result = api.v1.CollectionApi.createCollection()(request(tokenId, json))
      status(result) === UNAUTHORIZED
    }

    "create a collection in an organization with write permission" in new TestOPlenty(Permission.Write) {
      val requestJson = AnyContentAsJson(Json.toJson(new ContentCollection("test")))
      val result = api.v1.CollectionApi.createCollection()(request(tokenId, requestJson))
      val json = Json.parse(contentAsString(result))
      Logger.debug(json.toString)
      val id = new ObjectId((json \ "id").as[String])
      (json \ ContentCollection.name).as[String] must beEqualTo("test")
    }

    "not create an item in a collection with read permission" in new TestOPlenty(Permission.Read, Permission.Read) {
      val toCreate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("collectionId" -> collection.id.toString))
      var fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=" + tokenId, FakeHeaders(), AnyContentAsJson(toCreate))
      var result = routeAndCall(fakeRequest).get
      status(result) must beEqualTo(UNAUTHORIZED)
    }

    "create an item in a collection with write permission" in new TestOPlenty(Permission.Read, Permission.Write) {
      val title = "title"
      val toCreate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("collectionId" -> collection.id.toString, Keys.title -> title))
      var fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=" + tokenId, FakeHeaders(), AnyContentAsJson(toCreate))
      var result = routeAndCall(fakeRequest).get
      val json = Json.parse(contentAsString(result))
      status(result) === OK
      (json \ Keys.title).as[String] === title
    }
  }

}
