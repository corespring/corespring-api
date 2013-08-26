package tests.models

import org.corespring.test.BaseTest
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.libs.json.{JsObject, JsString, JsArray, Json}


class ItemQueryTest extends BaseTest{

  "filter search for author in contributorDetails" in {
    val author = "New England Common Assessment Program"
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{author:\""+author+"\"}"), f = Some("{author:1}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = route(request).get
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
    val result = route(request).get
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
    val result = route(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    val jsonSuccess = json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(1)
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
    val result = route(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    val jsonSuccess = json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(1)
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
    val result = route(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    val jsonSuccess = json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(1)
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
  "filter search by itemType AND title" in {
    val title = "DEV"
    val itemType = "Multiple Choice"
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{title:{$regex:\""+title+"\"},itemType:\""+itemType+"\"}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = route(request).get
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
    val result = route(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    val jsonSuccess = json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(1)
        jsobjects.forall(jsobj => {
          val result = ((jsobj \ "primarySubject") match {
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
          result
        })
      }
      case _ => false
    }
    jsonSuccess must beTrue
  }
  "search by title OR primarySubject OR gradeLevel OR itemType OR standards.dotNotation OR contributorDetails.author returns results even with a value for standards.dotNotation that does not contain any results" in  {
    val primarySubjectCategory = "Mathematics"
    val title = "DEV"
    val gradeLevel = "02"
    val itemType = "Multiple Choice"
    val standardsDotNotation = "blergl mergl"
    val author = "New England Common Assessment Program"
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{\"$or\":[{primarySubject.category:\""+primarySubjectCategory+"\"},{title:{$regex:\""+title+"\"}},{gradeLevel:\""+gradeLevel+"\"},{itemType:\""+itemType+"\"},{standards.dotNotation:\""+standardsDotNotation+"\"},{author:\""+author+"\"}]}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = route(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    val jsonSuccess = json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(1)
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
    val result = route(request).get
    status(result) must equalTo(BAD_REQUEST)
  }
  "filtering by data results in error" in {
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{data.name:meh}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = route(request).get
    status(result) must equalTo(BAD_REQUEST)
  }
  "filter items based on a set of subjects using $in" in {
    val category1 = "Mathematics"
    val category2 = "English Language Arts"
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{primarySubject.category:{$in : [\""+category1+"\",\""+category2+"\"]}}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = route(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    val jsonSuccess = json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(1)
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
  "filter items based on a set of collections using $in" in {
    val collection1 = "51114b127fc1eaa866444647"
    val collection2 = "511276834924c9ca07b97044"
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{collectionId:{$in:[\""+collection1+"\",\""+collection2+"\"]}}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = route(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    val jsonSuccess = json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(1)
        jsobjects.forall(jsobj => {
          (jsobj \ "collectionId") match {
            case JsString(collectionId) => collectionId == collection1 || collectionId == collection2
            case _ => false
          }
        })
      }
      case _ => false
    }
    jsonSuccess must beTrue
  }
  val searchParams:Seq[(String,String)] = Seq(
    "standards.dotNotation" -> "\"meh\"",
    "standards.category" -> "\"meh\"",
    "standards.standard" -> "\"meh\"",
    "standards.subject" -> "\"meh\"",
    "standards.guid" -> "\"meh\"",
    "standards.subCategory" -> "\"meh\"",
    "workflow.setup" -> "true",
    "workflow.tagged" -> "true",
    "workflow.standardsAligned" -> "true",
    "workflow.qaReview" -> "true",
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
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{"+searchParams.tail.foldRight[String](searchParams.head._1+":"+searchParams.head._2)((param,result) => result+","+param._1+":"+param._2)+"}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = route(request).get
    status(result) must equalTo(OK)
  }
  "all search field item params are checked within it's search method" in {
    val call:Call = api.v1.routes.ItemApi.list(f = Some("{"+searchParams.tail.foldRight[String](searchParams.head._1+":1")((param,result) => result+","+param._1+":1")+",id:1}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = route(request).get
    status(result) must equalTo(OK)
  }
  "search for items that do not have an itemType equal to Multiple Choice" in {
    val itemType = "Multiple Choice"
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{itemType : {$ne : \""+itemType+"\"}}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = route(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    val jsonSuccess = json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(1)
        jsobjects.forall(jsobj => {
          (jsobj \ "itemType") match {
            case JsString(jsitemType) => jsitemType != itemType
            case _ => true
          }
        })
      }
      case _ => false
    }
    jsonSuccess must beTrue
  }
  "search for items that do not contain a gradeLevel of 02 or 04" in {
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{gradeLevel:{$nin:[\"02\",\"04\"]}}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = route(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    val jsonSuccess = json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(1)
        jsobjects.forall(jsobj => {
          (jsobj \ "gradeLevel") match {
            case JsArray(grades) => grades.forall(jsgrade => jsgrade match {
              case JsString(grade) => grade != "02" && grade != "04"
              case _ => false
            })
            case _ => true
          }
        })
      }
      case _ => false
    }
    jsonSuccess must beTrue
  }
  "no results are returned when searching for itemType that has both values of Multiple Choice and Project" in {
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{$and:[{itemType:\"Multiple Choice\"},{itemType:\"Project\"}]}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = route(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beEqualTo(0)
      }
      case _ => failure("did not return an array")
    }
  }
  "match all items that have a gradeLevel which contains the values 02 and 04 in its array" in {
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{$and : [ { gradeLevel : \"02\"}, { gradeLevel : \"04\"}],gradeLevel:{$exists:true}}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = route(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    val jsonSuccess = json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(1)
        jsobjects.forall(jsobj => {
          (jsobj \ "gradeLevel") match {
            case JsArray(grades) => grades.exists(_.as[String] == "02") && grades.exists(_.as[String] == "04")
            case _ => false
          }
        })
      }
      case _ => false
    }
    jsonSuccess must beTrue
  }

  "search for items by metadata" in {
    val call:Call = api.v1.routes.ItemApi.list(q = Some("{extended.demo_org.ui_comment:{$regex:\"comment about ui\"}}"),f=Some("{extended:1}"))
    val request = FakeRequest(call.method,call.url+"&access_token="+token)
    val result = route(request).get
    status(result) must equalTo(OK)
    val json = Json.parse(contentAsString(result))
    val jsonSuccess = json match {
      case JsArray(jsobjects) => {
        jsobjects.size must beGreaterThanOrEqualTo(1)
        jsobjects.forall(jsobj => {
          (jsobj \ "extended") match {
            case JsObject(extended) => extended.find(field => {
              field._1 == "demo_org" && (field._2 match {
                case JsObject(props) => props.find(prop => prop._1 == "ui_comment" && prop._2 == JsString("comment about ui")).isDefined
                case _ => false
              })
            }).isDefined
            case _ => false
          }
        })
      }
      case _ => false
    }
    jsonSuccess must beTrue
  }
}
