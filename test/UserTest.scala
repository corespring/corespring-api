import play.api.libs.json.{JsValue, Json}
import play.api.Logger
import play.api.mvc.AnyContentAsJson
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._

/**
 * User API Tests
 */

object UserTest extends BaseTest {
  val userId = "502d46ce0364068384f217a3"

  "list all visible users" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/users?access_token=%s".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val users = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    users must have size 3
  }


  "list all visible users skipping the first result" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/users?access_token=%s&sk=1".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val users = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    users must have size 2
    (users(0) \ "userName").as[String] must beEqualTo("marge")
  }


  "list all visible users limit results to 2" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/users?access_token=%s&l=2".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val users = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    users must have size 2
  }

  "list all visible users returning only the id and userName fields" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/users?access_token=%s&f={\"userName\":1}".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val users = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    users.foreach( u => {
      (u \ "userName").asOpt[String] must beSome
      (u \ "fullName").asOpt[String] must beNone
      (u \ "email").asOpt[String] must beNone
    })
    users must have size 3
  }

  "find a user with userName equal to 'homer'" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/users?access_token=%s&q={\"userName\":\"homer\"}".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val users = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    users must have size 1
    (users(0) \ "userName").as[String] must beEqualTo("homer")
  }

  "find user with id %s".format(userId) in {
    val fakeRequest = FakeRequest(GET, "/api/v1/users/%s?access_token=%s".format(userId, token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val user = Json.fromJson[JsValue](Json.parse(contentAsString(result)))
    (user \ "id").as[String] must beEqualTo(userId)
    (user \ "userName").as[String] must beEqualTo("homer")

  }

  "create, update and delete a user" in {
    val name = "john"
    val fullName = "John Doe"
    val email = "john.doe@corespring.org"

    // create it
    val toCreate = Map("userName" -> name, "fullName" -> fullName, "email" -> email)
    val fakeRequest = FakeRequest(POST, "/api/v1/users?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(Json.toJson(toCreate)))
    val r = routeAndCall(fakeRequest)
    if ( r.isEmpty ) {
      failure("Failed to create user")
    }
    val result = r.get
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val user = Json.fromJson[JsValue](Json.parse(contentAsString(result)))
    (user \ "userName").as[String] must beEqualTo(name)
    (user \ "fullName").as[String] must beEqualTo(fullName)
    (user \ "email").as[String] must beEqualTo(email)

    // update
    val name2 = "jane"
    val fullName2 = "Jane Doe"
    val email2 = "jane.doe@corespring.org"

    val toUpdate = Map("userName" -> name2, "fullName" -> fullName2, "email" -> email2)
    val userId = (user \ "id").as[String]
    val postRequest = FakeRequest(PUT, "/api/v1/users/%s?access_token=%s".format( userId, token), FakeHeaders(), AnyContentAsJson(Json.toJson(toUpdate)))
    routeAndCall(postRequest) match {
      case Some(result2) => {
        status(result2) must equalTo(OK)
        charset(result2) must beSome("utf-8")
        contentType(result2) must beSome("application/json")
        val updatedUser = Json.fromJson[JsValue](Json.parse(contentAsString(result2)))
        (updatedUser \ "id").as[String] must beEqualTo( userId )
        (updatedUser \ "userName").as[String] must beEqualTo(name2)
        (updatedUser \ "fullName").as[String] must beEqualTo(fullName2)
        (updatedUser \ "email").as[String] must beEqualTo(email2)

        // delete
        val deleteRequest = FakeRequest(DELETE, "/api/v1/users/%s?access_token=%s".format(userId, token))
        val Some(result3) = routeAndCall(deleteRequest)
        status(result3) must equalTo(OK)
        charset(result3) must beSome("utf-8")
        contentType(result3) must beSome("application/json")

        val Some(result4) = routeAndCall(FakeRequest(GET, "/api/v1/users/%s?access_token=%s".format(userId, token)))
        status(result4) must equalTo(NOT_FOUND)
      }
      case None => failure("failed to update user")
    }
  }
}
