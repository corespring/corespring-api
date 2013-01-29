package tests.models

import tests.BaseTest
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.libs.json.{JsObject, JsString, JsArray, Json}
import controllers.Log


class ItemQueryTest extends BaseTest{

  "filter search for author in contributorDetails" in {
    val author = "New England Common Assessment Program"
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{author:\""+author+"\"}"), f = Some("{author:1}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = routeAndCall(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    val jsonSuccess = json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(1)
        jsobjects.forall(jsobj => {
          (jsobj \ "author") match {
            case JsString(jsauthor) => jsauthor == author
            case _ => false
          }
        })
      }
      case _ => false
    }
    jsonSuccess must beTrue
  }
  "filter search by gradeLevel matching multiple grades" in {
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{gradeLevel:{$all:[\"02\",\"04\"]}}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = routeAndCall(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    val jsonSuccess = json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(1)
        jsobjects.forall(jsobj => {
          (jsobj \ "gradeLevel") match {
            case JsArray(jsGradeLevel) => jsGradeLevel.contains(JsString("02")) && jsGradeLevel.contains(JsString("04"))
            case _ => false
          }
        })
      }
      case _ => false
    }
    jsonSuccess must beTrue
  }
  "filter search by subCategory in standards" in {
    val subCategory = "Key Ideas and Details"
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{standards.subCategory:\""+subCategory+"\"}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = routeAndCall(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    val jsonSuccess = json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(5)
        jsobjects.forall(jsobj => {
          (jsobj \ "standards") match {
            case JsArray(standards) => standards.exists(_ match {
              case JsObject(props) => props.contains("subCategory" -> JsString(subCategory))
              case _ => false
            })
            case _ => false
          }
        })
      }
      case _ => false
    }
    jsonSuccess must beTrue
  }
  "filter search by subjects" in {
    val primarySubjectCategory = "Mathematics"
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{primarySubject.category:\""+primarySubjectCategory+"\"}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = routeAndCall(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    val jsonSuccess = json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(2)
        jsobjects.forall(jsobj => {
          (jsobj \ "primarySubject") match {
            case JsObject(props) => props.contains("category" -> JsString(primarySubjectCategory))
            case _ => false
          }
        })
      }
      case _ => false
    }
    jsonSuccess must beTrue
  }
  "use regex to filter search by title" in {
    val title = "DEV"
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{title:{$regex:\""+title+"\"}}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = routeAndCall(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    val jsonSuccess = json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(2)
        jsobjects.forall(jsobj => {
          (jsobj \ "title") match {
            case JsString(jstitle) => jstitle.indexOf(title) != -1
            case _ => false
          }
        })
      }
      case _ => false
    }
    jsonSuccess must beTrue
  }
  "filter search by gradeLevel AND title" in {
    val title = "DEV"
    val itemType = "Multiple Choice"
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{title:{$regex:\""+title+"\"},itemType:\""+itemType+"\"}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = routeAndCall(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    val jsonSuccess = json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(1)
        jsobjects.forall(jsobj => {
          ((jsobj \ "title") match {
            case JsString(jstitle) => jstitle.indexOf(title) != -1
            case _ => false
          }) && ((jsobj \ "itemType") match {
            case JsString(jsitemType) => jsitemType == itemType
            case _ => false
          })
        })
      }
      case _ => false
    }
    jsonSuccess must beTrue
  }
  "filter search by title OR primarySubject OR gradeLevel OR itemType OR standards.dotNotation OR contributorDetails.author" in  {
    val primarySubjectCategory = "Mathematics"
    val title = "DEV"
    val gradeLevel = "02"
    val itemType = "Multiple Choice"
    val standardsDotNotation = "RI.3.1"
    val author = "New England Common Assessment Program"
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{\"$or\":[{primarySubject.category:\""+primarySubjectCategory+"\"},{title:{$regex:\""+title+"\"}},{gradeLevel:\""+gradeLevel+"\"},{itemType:\""+itemType+"\"},{standards.dotNotation:\""+standardsDotNotation+"\"},{author:\""+author+"\"}]}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = routeAndCall(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    val jsonSuccess = json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(5)
        jsobjects.forall(jsobj => {
          ((jsobj \ "primarySubject") match {
            case JsObject(props) => props.contains("category" -> JsString(primarySubjectCategory))
            case _ => false
          }) || ((jsobj \ "title") match {
            case JsString(jstitle) => jstitle.contains(title)
            case _ => false
          }) || ((jsobj \ "gradeLevel") match {
            case JsArray(jsgrades) => jsgrades.exists(jsgrade => jsgrade.as[String] == gradeLevel)
            case _ => false
          }) || ((jsobj \ "itemType") match {
            case JsString(jsitemType) => jsitemType == itemType
            case _ => false
          }) || ((jsobj \ "standards") match {
            case JsObject(props) => props.contains("dotNotation" -> JsString(standardsDotNotation))
            case _ => false
          }) || ((jsobj \ "author") match {
            case JsString(jsauthor) => jsauthor == author
            case _ => false
          })
        })
      }
      case _ => false
    }
    jsonSuccess must beTrue
  }
  "filtering by supportingMaterials results in error" in {
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{supportingMaterials.name:meh}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = routeAndCall(request).get
    status(result) must equalTo(BAD_REQUEST)
  }
  "filtering by data results in error" in {
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{data.name:meh}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = routeAndCall(request).get
    status(result) must equalTo(BAD_REQUEST)
  }
  "filter items based on a set of subjects using $in" in {
    val category1 = "Mathematics"
    val category2 = "English Language Arts"
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{primarySubject.category:{$in : [\""+category1+"\",\""+category2+"\"]}}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = routeAndCall(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    val jsonSuccess = json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(2)
        jsobjects.exists(jsobj => {
          (jsobj \ "primarySubject") match {
            case JsObject(props) => props.contains("category" -> JsString(category1))
            case _ => false
          }
        }) && jsobjects.exists(jsobj => {
          (jsobj \ "primarySubject") match {
            case JsObject(props) => props.contains("category" -> JsString(category2))
            case _ => false
          }
        })
      }
      case _ => false
    }
    jsonSuccess must beTrue
  }
  val params:Seq[(String,String)] = Seq(
    "standards.dotNotation" -> "\"meh\"",
    "standards.category" -> "\"meh\"",
    "standards.standard" -> "\"meh\"",
    "standards.subject" -> "\"meh\"",
    "standards.guid" -> "\"meh\"",
    "standards.subCategory" -> "\"meh\"",
    "worflow.setup" -> "true",
    "worflow.tagged" -> "true",
    "worflow.standardsAligned" -> "true",
    "worflow.qaReview" -> "true",
    "author" -> "\"meh\"",
    "contributor" -> "\"meh\"",
    "costForResource" -> "\"meh\"",
    "credentials" -> "\"meh\"",
    "licenseType" -> "\"meh\"",
    "sourceUrl" -> "\"meh\"",
    "copyrightOwner" -> "\"meh\"",
    "copyrightYear" -> "\"meh\"",
    "copyrightExpirationDate" -> "\"meh\"",
    "copyrightImageName" -> "\"meh\"",
    "lexile" -> "\"meh\"",
    "demonstratedKnowledge" -> "\"meh\"",
    "originId" -> "\"meh\"",
    "collectionId" -> "\"meh\"",
    "contentType" -> "\"meh\"",
    "pValue" -> "\"meh\"",
    "relatedCurriculum" -> "\"meh\"",
    "gradeLevel" -> "\"meh\"",
    "itemType" -> "\"meh\"",
    "keySkills" -> "\"meh\"",
    "primarySubject.subject" -> "\"meh\"",
    "primarySubject.category" -> "\"meh\"",
    "relatedSubject.subject" -> "\"meh\"",
    "relatedSubject.category" -> "\"meh\"",
    "priorUse" -> "\"meh\"",
    "priorGradeLevel" -> "\"meh\"",
    "reviewsPassed" -> "\"meh\"",
    "title" -> "\"meh\""
  )

  "all queryable item params are checked within it's search method" in {
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{"+params.tail.foldRight[String](params.head._1+":"+params.head._2)((param,result) => result+","+param._1+":"+param._2)+"}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = routeAndCall(request).get
    status(result) must equalTo(OK)
  }
  "all search field item params are checked within it's search method" in {
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{"+params.tail.foldRight[String](params.head._1+":"+params.head._2)((param,result) => result+","+param._1+":"+param._2)+"}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = routeAndCall(request).get
    status(result) must equalTo(OK)
  }
  "search items, excluding 'standards.subject' includes everything except the subject portion of the standard" in {
    pending
  }
  "search items, excluding 'primarySubject.category' includes everything except the category portion of the primary subject" in {
    pending
  }
  "able to sort by grade within item" in {
    pending
  }
}
