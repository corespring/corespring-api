package org.corespring.api.v1

import org.bson.types.ObjectId
import org.corespring.models.{ Standard, Subject }
import org.corespring.models.item.FieldValue
import org.corespring.models.json.JsonFormatting
import org.corespring.services.{ StandardQuery, SubjectQuery, SubjectService, StandardService }
import org.corespring.test.JsonAssertions
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ AnyContent, Action }
import play.api.test.{ FakeRequest, PlaySpecification }
import scala.concurrent.duration._

import scala.concurrent.{ Await, Future, ExecutionContext }

class FieldValuesApiTest
  extends PlaySpecification
  with Mockito
  with JsonAssertions {

  trait scope extends Scope {

    def queryJson: Option[JsValue] = None
    def queryString = queryJson.map(Json.stringify(_))
    val v2 = mock[org.corespring.v2.api.FieldValuesApi]

    val standardService = {
      val m = mock[StandardService]
      m.query(any[StandardQuery], any[Int], any[Int]) returns Stream.empty[Standard]
      m.list(any[Int], any[Int]) returns Stream.empty[Standard]
      m
    }

    val subjectService = {
      val m = mock[SubjectService]
      m.query(any[SubjectQuery], any[Int], any[Int]) returns Stream.empty[Subject]
      m.list(any[Int], any[Int]) returns Stream.empty[Subject]
      m
    }

    val jsonFormatting = new JsonFormatting {
      override def findStandardByDotNotation: (String) => Option[Standard] = _ => None

      override def rootOrgId: ObjectId = ObjectId.get

      override def fieldValue: FieldValue = FieldValue()

      override def findSubjectById: (ObjectId) => Option[Subject] = _ => None
    }

    protected def wait[A](f: Future[A]) = Await.result(f, 30.second)

    val ec = new V1ApiExecutionContext(ExecutionContext.global)
    val api = new FieldValuesApi(v2, standardService, subjectService, jsonFormatting, ec)

    val req = FakeRequest("", "")

    def regex(s: String) = Json.obj("$regex" -> s"\\\\b$s")
  }

  def assertBasics(fn: FieldValuesApi => (Option[String], Int, Int) => Action[AnyContent]) = {

    "call list if there's no query" in new scope {
      val result = fn(api)(None, 0, 0)(req)
      wait(result)
      there was one(subjectService).list(any[Int], any[Int])
    }

    "return an error if the $or has different values" in new scope {

      override val queryJson = Some(Json.obj(
        "$or" -> Json.arr(
          Json.obj("subject" -> Json.obj("$regex" -> "\\\\bApple")),
          Json.obj("standard" -> Json.obj("$regex" -> "\\\\bBanana")))))
      val result = api.standard(queryString, 0, 0)(req)
      status(result) === BAD_REQUEST
      //Note: this should never happen and is temporary so hard coding the error string is ok.
      contentAsString(result) === "The regex values differ: \\\\bApple, \\\\bBanana"
    }
  }

  "subject" should {

    assertBasics(api => api.subject)

    "map the old json format to a SubjectQuery" in new scope {

      override val queryJson = Some(
        Json.obj("$or" ->
          Json.arr(
            Json.obj("subject" -> regex("a")))))

      val result = api.subject(queryString, 0, 0)(req)
      val query = SubjectQuery("a", None, None)
      wait(result)
      there was one(subjectService).query(query, 0, 0)
    }
  }

  "standard" should {

    assertBasics(api => api.subject)

    "map the old json format to a SubjectQuery" in new scope {

      override val queryJson = Some(
        Json.obj(
          "category" -> "category",
          "subCategory" -> "subcategory",
          "standard" -> "standard",
          "subject" -> "subject",
          "$or" ->
            Json.arr(
              Json.obj("subject" -> regex("a")))))

      val result = api.standard(queryString, 0, 0)(req)
      val query = StandardQuery("a", Some("standard"), Some("subject"), Some("category"), Some("subcategory"))
      wait(result)
      there was one(standardService).query(query, 0, 0)
    }
  }

}
