package tests.api.v1

import org.specs2.mutable.Specification
import play.api.libs.json.{JsValue, Json}
import play.api.Logger
import play.api.mvc.AnyContentAsJson
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import scala.Some
import play.api.test.FakeHeaders
import play.api.mvc.AnyContentAsJson
import scala.Some
import tests.BaseTest

/**
 *
 */
class OrganizationApiTest extends BaseTest {
  val orgId = "51114b307fc1eaa866444648"

  "list all visible organizations" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations?access_token=%s".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val organizations = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    organizations must have size 3
  }

  "list all visible organizations skipping 2 results" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations?access_token=%s&sk=2".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val organizations = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    organizations must have size 1
  }

  "list all visible organizations limit results to 2" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations?access_token=%s&l=2".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    Json.fromJson[List[JsValue]](Json.parse(contentAsString(result))) must have size 2
  }

  "list all visible organizations returning only the id and name fields" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations?access_token=%s&f={\"name\":1}".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val organizations = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    organizations.foreach(o => {
      (o \ "path").asOpt[String] must beNone
    })
    organizations must have size 3
  }

  "list organization tree" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations/%s/tree?access_token=%s".format(orgId, token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val tree = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    tree.foreach(o => {
      // make sure the orgId is in the path of each retrieved org
      (o \ "path").as[Seq[String]].contains(orgId) must beTrue
    })
  }

  "list organization children" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations/%s/children?access_token=%s".format(orgId, token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val children = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    children.foreach(o => {
      // make sure the orgId is in the path of each retrieved org
      (o \ "path").as[Seq[String]].contains(orgId) must beTrue
    })
    // make sure the parent org was not returned
    children.foreach(o => {
      (o \ "id").as[String] must not equalTo (orgId)
    })
  }

  "find an organization with name 'Demo Organization 3'" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations?access_token=%s&q={\"name\":\"Demo Organization 3\"}".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val organizations = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    organizations must have size 1
    (organizations(0) \ "name").as[String] must beEqualTo("Demo Organization 3")
  }

  "get the default organization" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations/default?access_token=%s".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val organization = Json.fromJson[JsValue](Json.parse(contentAsString(result)))
    (organization \ "id").as[String] must beEqualTo(orgId)
    (organization \ "name").as[String] must beEqualTo("Demo Organization")

  }


  "find an organization with id %s".format(orgId) in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations/%s?access_token=%s".format(orgId, token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val organization = Json.fromJson[JsValue](Json.parse(contentAsString(result)))
    (organization \ "id").as[String] must beEqualTo(orgId)
    (organization \ "name").as[String] must beEqualTo("Demo Organization")

  }

}
