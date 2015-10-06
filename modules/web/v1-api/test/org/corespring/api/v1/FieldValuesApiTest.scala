package org.corespring.api.v1

import org.bson.types.ObjectId
import org.corespring.models.{ Standard, Subject }
import org.corespring.models.item.FieldValue
import org.corespring.models.json.JsonFormatting
import org.corespring.services.{ StandardQuery, SubjectQuery, SubjectService, StandardService }
import org.corespring.test.JsonAssertions
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json, JsObject }
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
      m
    }

    val subjectService = {
      val m = mock[SubjectService]
      m.query(any[SubjectQuery], any[Int], any[Int]) returns Stream.empty[Subject]
      m
    }

    val jsonFormatting = new JsonFormatting {
      override def findStandardByDotNotation: (String) => Option[Standard] = _ => None

      override def countItemsInCollection(collectionId: ObjectId): Long = 0

      override def rootOrgId: ObjectId = ObjectId.get

      override def fieldValue: FieldValue = FieldValue()

      override def findSubjectById: (ObjectId) => Option[Subject] = _ => None
    }

    protected def wait[A](f: Future[A]) = Await.result(f, 1.second)

    val ec = new V1ApiExecutionContext(ExecutionContext.global)
    val api = new FieldValuesApi(v2, standardService, subjectService, jsonFormatting, ec)

    val req = FakeRequest("", "")

    def regex(s: String) = Json.obj("$regex" -> s"\\\\b$s")
  }

  "subject" should {

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

}
