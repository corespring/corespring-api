package tests.api.v1

import play.api.libs.json.JsValue
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.Some
import tests.BaseTest

class OrganizationApiTest extends BaseTest {
  val orgId = "51114b307fc1eaa866444648"

  "list all visible organizations" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations?access_token=%s".format(token))
    val Some(result) = route(fakeRequest)
    assertResult(result)
    assertResult(result)
    val organizations = parsed[List[JsValue]](result)
    organizations must have size 3
  }

  "list all visible organizations skipping 2 results" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations?access_token=%s&sk=2".format(token))
    val Some(result) = route(fakeRequest)
    assertResult(result)
    val organizations = parsed[List[JsValue]](result)
    organizations must have size 1
  }

  "list all visible organizations limit results to 2" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations?access_token=%s&l=2".format(token))
    val Some(result) = route(fakeRequest)
    assertResult(result)
    parsed[List[JsValue]](result) must have size 2
  }

  "list all visible organizations returning only the id and name fields" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations?access_token=%s&f={\"name\":1}".format(token))
    val Some(result) = route(fakeRequest)
    assertResult(result)
    val organizations = parsed[List[JsValue]](result)
    organizations.foreach(o => {
      (o \ "path").asOpt[String] must beNone
    })
    organizations must have size 3
  }

  "list organization tree" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations/%s/tree?access_token=%s".format(orgId, token))
    val Some(result) = route(fakeRequest)
    assertResult(result)
    val tree = parsed[List[JsValue]](result)
    tree.foreach(o => {
      // make sure the orgId is in the path of each retrieved org
      (o \ "path").as[Seq[String]].contains(orgId) must beTrue
    })
  }

  "list organization children" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations/%s/children?access_token=%s".format(orgId, token))
    val Some(result) = route(fakeRequest)
    assertResult(result)
    val children = parsed[List[JsValue]](result)
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
    val Some(result) = route(fakeRequest)
    assertResult(result)
    val organizations = parsed[List[JsValue]](result)
    organizations must have size 1
    (organizations(0) \ "name").as[String] must beEqualTo("Demo Organization 3")
  }

  "get the default organization" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations/default?access_token=%s".format(token))
    val Some(result) = route(fakeRequest)
    assertResult(result)
    val organization = parsed[JsValue](result)
    (organization \ "id").as[String] must beEqualTo(orgId)
    (organization \ "name").as[String] must beEqualTo("Demo Organization")

  }


  "find an organization with id %s".format(orgId) in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations/%s?access_token=%s".format(orgId, token))
    val Some(result) = route(fakeRequest)
    assertResult(result)
    val organization = parsed[JsValue](result)
    (organization \ "id").as[String] must beEqualTo(orgId)
    (organization \ "name").as[String] must beEqualTo("Demo Organization")

  }

}
