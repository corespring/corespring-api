package org.corespring.v2.api

import com.fasterxml.jackson.core.JsonParseException
import org.corespring.itemSearch.ItemIndexService
import org.corespring.models.{ Standard, Subject }
import org.corespring.services.{ StandardQuery, StandardService, SubjectQuery, SubjectService }
import org.corespring.v2.actions.V2ActionsFactory
import org.mockito.Matchers
import org.specs2.specification.{ Fragment, Scope }
import play.api.libs.json.Json
import play.api.mvc.{ Action, AnyContent }
import play.api.test.FakeRequest

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.Success

class FieldValuesApiTest extends V2ApiSpec {

  import ExecutionContext.Implicits.global

  val contributorValues = Seq("these", "are", "contributors")
  val gradeValues = Seq("these are grades")

  class scope extends Scope with V2ApiScope {

    lazy val itemIndexService = {
      val m = mock[ItemIndexService]
      m.distinct(Matchers.eq("contributorDetails.contributor"), any[Seq[String]]) returns Future { Success(contributorValues) }
      m.distinct(Matchers.eq("taskInfo.gradeLevel"), any[Seq[String]]) returns Future { Success(gradeValues) }
      m
    }

    lazy val standardService = {
      val m = mock[StandardService]
      m.list(any[Int], any[Int]) returns Stream.empty[Standard]
      m.query(any[StandardQuery], any[Int], any[Int]) returns Stream.empty[Standard]
      m
    }

    lazy val subjectService = {
      val m = mock[SubjectService]
      m.list(any[Int], any[Int]) returns Stream.empty[Subject]
      m.query(any[SubjectQuery], any[Int], any[Int]) returns Stream.empty[Subject]
      m
    }

    val api = new FieldValuesApi(
      V2ActionsFactory.apply(),
      itemIndexService,
      v2ApiContext,
      standardService,
      subjectService,
      jsonFormatting)

    val req = FakeRequest("", "")
  }

  def assertErrors(fn: FieldValuesApi => (Option[String], Int, Int) => Action[AnyContent]): Fragment = {

    def jsonError(s: String) = try {
      Json.parse(s)
      "no-error"
    } catch {
      case jpe: JsonParseException => jpe.getMessage
    }

    "fails if the query string isn't valid json" in new scope {
      val result = fn(api)(Some("BAD-JSON"), 0, 0)(req)
      status(result) === 400
      contentAsString(result) === jsonError("BAD-JSON")
    }

    "fails if the json can't be read as a Query" in new scope {
      val result = fn(api)(Some("{}"), 0, 0)(req)
      status(result) === 400
      contentAsString(result) === "Json can't be read as a query: {}"
    }
  }

  "standard" should {
    assertErrors(api => api.standard)
  }

  "subject" should {

    assertErrors(api => api.subject)

    "call subjectService.query" in new scope {
      val query = SubjectQuery("b", None, None)
      val json = Json.writes[SubjectQuery].writes(query)
      val result = api.subject(Some(Json.stringify(json)), 0, 0)(req)
      status(result) === 200
      there was one(subjectService).query(query, 0, 0)
    }
  }

  "contributors" should {

    "return 200" in new scope() {
      status(api.contributors()(FakeRequest())) === OK
    }

    "return contributors from ItemIndexService" in new scope() {
      val json = contentAsJson(api.contributors()(FakeRequest()))
      json.as[Seq[String]] === contributorValues
    }
  }

  "gradeLevels" should {

    "return 200" in new scope() {
      status(api.gradeLevels()(FakeRequest())) === OK
    }

    "return gradeLevels from itemIndexService" in new scope() {
      val json = contentAsJson(api.gradeLevels()(FakeRequest()))
      json.as[Seq[String]] === gradeValues
    }
  }
}
