package org.corespring.v2.player.hooks

import org.corespring.models.{ Standard, Subject }
import org.corespring.services.{ StandardQuery, StandardService, QueryService, SubjectQuery }
import org.corespring.v2.player.V2PlayerIntegrationSpec
import org.specs2.mock.Mockito
import org.specs2.specification.{ Fragment, Scope }
import play.api.libs.json.{ JsObject, Json }
import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._

class DataQueryHooksTest extends V2PlayerIntegrationSpec with Mockito {

  trait scope extends Scope with StubJsonFormatting {

    def queryJson: Option[JsObject] = Some(Json.obj())

    def queryString = queryJson.map(Json.stringify(_))

    val subjectQueryService = {
      val m = mock[QueryService[Subject, SubjectQuery]]
      m.query(any[SubjectQuery], any[Int], any[Int]) returns Stream.empty[Subject]
      m.list returns Stream.empty[Subject]
      m
    }

    val standardService = {
      val m = mock[StandardService]
      m.query(any[StandardQuery], any[Int], any[Int]) returns Stream.empty[Standard]
      m.list returns Stream.empty[Standard]
      m
    }

    val standardsTree: StandardsTree = new StandardsTree(Json.arr())

    def wait[A](f: Future[A]) = Await.result(f, 1.second)

    val hooks = new DataQueryHooks(
      subjectQueryService,
      standardService,
      standardsTree,
      jsonFormatting,
      containerExecutionContext)
  }

  "list" should {

    "when listing subjects" should {

      val jsonWithFilters = Json.obj(
        "searchTerm" -> "a",
        "filters" ->
          Json.obj("subject" -> "History",
            "category" -> "Humanities"))

      trait subjects extends scope {
        def key: String

        def expected: SubjectQuery

        val result = hooks.list(key, queryString)
        wait(result)
        there was one(subjectQueryService).query(expected, 0, 0)
      }

      def assert(subjectKey: String): Fragment = {

        class s(override val queryJson: Option[JsObject],
          override val expected: SubjectQuery) extends subjects {
          override lazy val key = s"subjects.$subjectKey"
        }

        "call list if there's no query" in new scope {
          val future = hooks.list(s"subjects.$subjectKey", None)
          val result = wait(future)
          there was one(subjectQueryService).list()
        }

        s"return an error if it's bad json for subjects.$subjectKey" in new scope {
          val future = hooks.list(s"subjects.$subjectKey", Some("BAD-JSON"))
          val result = wait(future)
          result === Left(BAD_REQUEST, _: String)
        }

        s"call subjectQueryService.query for subjects.$subjectKey" in new s(
          Some(Json.obj("searchTerm" -> "a")),
          SubjectQuery("a", None, None))

        s"call subjectQueryService.query for subjects.$subjectKey with filters" in new s(
          Some(jsonWithFilters),
          SubjectQuery("a", Some("History"), Some("Humanities")))

      }

      "when listing subjects.primary" should assert("primary")
      "when listing subjects.related" should assert("related")
    }

    "when listing standards" should {

      val jsonWithFilters = Json.obj("searchTerm" -> "a",
        "filters" -> Json.obj(
          "category" -> "C",
          "subCategory" -> "SC",
          "standard" -> "S",
          "subject" -> "SU"))

      class standards(override val queryJson: Option[JsObject],
        val expected: StandardQuery) extends scope {
        val result = hooks.list("standards", queryString)
        wait(result)
        there was one(standardService).query(expected, 0, 0)
      }

      "call list if there's no query" in new scope {
        val future = hooks.list("standards", None)
        val result = wait(future)
        there was one(standardService).list()
      }

      "return an error if it's bad json" in new scope {
        val future = hooks.list("standards", Some("BAD-JSON"))
        val result = wait(future)
        result === Left(BAD_REQUEST, _: String)
      }

      "call standardService.query" in new standards(
        Some(Json.obj("searchTerm" -> "a")),
        StandardQuery("a", None, None, None, None))

      "call standardService.query with filters" in new standards(
        Some(jsonWithFilters),
        StandardQuery("a", Some("S"), Some("SU"), Some("C"), Some("SC")))

    }
  }
}
