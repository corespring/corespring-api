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
  val orgId = "502404dd0364dc35bb393397"

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
    (organizations(0) \ "name").as[String] must beEqualTo("Organization G")
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

  "find an organization with name 'Organization D'" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations?access_token=%s&q={\"name\":\"Organization+D\"}".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val organizations = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    organizations must have size 1
    (organizations(0) \ "name").as[String] must beEqualTo("Organization D")
  }

  "get the default organization" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations/default?access_token=%s".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val organization = Json.fromJson[JsValue](Json.parse(contentAsString(result)))
    (organization \ "id").as[String] must beEqualTo(orgId)
    (organization \ "name").as[String] must beEqualTo("Organization G")

  }


  "find an organization with id %s".format(orgId) in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations/%s?access_token=%s".format(orgId, token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val organization = Json.fromJson[JsValue](Json.parse(contentAsString(result)))
    (organization \ "id").as[String] must beEqualTo(orgId)
    (organization \ "name").as[String] must beEqualTo("Organization G")

  }

//  "create, update and delete an organization" in {
//    val name = "Acme"
//
//    // create it
//    val toCreate = Map("name" -> name)
//    val fakeRequest = FakeRequest(POST, "/api/v1/organizations?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(Json.toJson(toCreate)))
//    val r = routeAndCall(fakeRequest)
//    if (r.isEmpty) {
//      failure("Failed to create an organization")
//    }
//    val result = r.get
//    status(result) must equalTo(OK)
//    charset(result) must beSome("utf-8")
//    contentType(result) must beSome("application/json")
//    val organization = Json.fromJson[JsValue](Json.parse(contentAsString(result)))
//    (organization \ "name").as[String] must beEqualTo(name)
//
//    // update
//    val name2 = "Acme Corp"
//    val toUpdate = Map("name" -> name2)
//    val orgId = (organization \ "id").as[String]
//    val postRequest = FakeRequest(PUT, "/api/v1/organizations/%s?access_token=%s".format(orgId, token), FakeHeaders(), AnyContentAsJson(Json.toJson(toUpdate)))
//    routeAndCall(postRequest) match {
//      case Some(result2) => {
//        status(result2) must equalTo(OK)
//        charset(result2) must beSome("utf-8")
//        contentType(result2) must beSome("application/json")
//        val updatedOrganization = Json.fromJson[JsValue](Json.parse(contentAsString(result2)))
//        (updatedOrganization \ "id").as[String] must beEqualTo(orgId)
//        (updatedOrganization \ "name").as[String] must beEqualTo(name2)
//
//        // delete
//        val deleteRequest = FakeRequest(DELETE, "/api/v1/organizations/%s?access_token=%s".format(orgId, token))
//        val Some(result3) = routeAndCall(deleteRequest)
//        status(result3) must equalTo(OK)
//        charset(result3) must beSome("utf-8")
//        contentType(result3) must beSome("application/json")
//
//        val Some(result4) = routeAndCall(FakeRequest(GET, "/api/v1/organizations/%s?access_token=%s".format(orgId, token)))
//        status(result4) must equalTo(NOT_FOUND)
//      }
//      case None => failure("failed to update an organization")
//    }
//  }

}
