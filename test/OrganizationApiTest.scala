import org.specs2.mutable.Specification
import play.api.libs.json.{JsValue, Json}
import play.api.Logger
import play.api.mvc.AnyContentAsJson
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import scala.Some

/**
 *
 */
class OrganizationApiTest extends BaseTest {
  "list all visible organizations" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations?access_token=%s".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    Logger.info("charset = " + charset(result))
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val organizations = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    organizations must have size 3
  }

  "list all visible organizations skipping 2 results" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations?access_token=%s&sk=2".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    Logger.info("charset = " + charset(result))
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
    Logger.info("charset = " + charset(result))
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    Json.fromJson[List[JsValue]](Json.parse(contentAsString(result))) must have size 2
  }

  "list all visible organizations returning only the id and name fields" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/organizations?access_token=%s&f={\"name\":1}".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    Logger.info("charset = " + charset(result))
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val organizations = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    organizations.foreach( o => {
      (o \ "path").asOpt[String] must beNone
    })
    organizations must have size 3
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

  val orgId = "502404dd0364dc35bb393397"

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


}
