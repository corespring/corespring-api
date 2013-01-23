package tests.models

import tests.BaseTest
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.libs.json.{JsObject, JsString, JsArray, Json}


class ItemQueryTest extends BaseTest{

  "filter search for author in contributorDetails" in {
    val author = "New England Common Assessment Program"
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{author:"+author+"}"))
    val request = FakeRequest(call.method,tokenize(call.url))
    val result = routeAndCall(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(5)
        jsobjects.forall(jsobj => {
          (jsobj \ "author") match {
            case JsString(jsauthor) => jsauthor == author
            case _ => false
          }
        }) must beTrue
      }
      case _ => failure
    }
  }
  "filter search by gradeLevel" in {
    val gradeLevel = "[\"02\"]"
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{gradeLevel:"+gradeLevel+"}"))
    val request = FakeRequest(call.method,tokenize(call.url))
    val result = routeAndCall(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(3)
        jsobjects.forall(jsobj => {
          (jsobj \ "gradeLevel") match {
            case JsArray(jsGradeLevel) => jsGradeLevel.contains(JsString("02"))
            case _ => false
          }
        }) must beTrue
      }
      case _ => failure
    }
  }
  "filter search by subCategory in standards" in {
    val subCategory = "Key Ideas and Details"
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{standards.subCategory:"+subCategory+"}"))
    val request = FakeRequest(call.method,tokenize(call.url))
    val result = routeAndCall(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(1)
        jsobjects.forall(jsobj => {
          (jsobj \ "standards") match {
            case JsObject(props) => props.contains("subCategory" -> JsString(subCategory))
            case _ => false
          }
        }) must beTrue
      }
      case _ => failure
    }
  }
  "filter search by subjects" in {
    pending
  }
  "use regex to filter search by title" in {
    pending
  }
  "filter search by lexile AND originId" in {
    pending
  }
  "filter search by workflow OR bloomsTaxonomy" in  {
    pending
  }
  "filtering by supportingMaterials or data results in error" in {
    pending
  }
  "filter items based on a set of subjects using $in" in {
    pending
  }
  "filter items that are not included in a certain collection using $ne" in {
    pending
  }
  "all queryable item params are checked within it's search method" in {
    pending
  }
  "all search field item params are checked within it's search method" in {
    pending
  }
  "search items, excluding 'standards.subject' includes everything except the subject portion of the standard" in {
    pending
  }
  "search items, excluding 'primarySubject.category' includes everything except the category portion of the primary subject" in {
    pending
  }
  "able to sort by any key within item" in {
    pending
  }
}
